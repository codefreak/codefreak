package de.code_freak.codefreak.service

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.exceptions.ImageNotFoundException
import com.spotify.docker.client.messages.HostConfig
import de.code_freak.codefreak.config.AppConfiguration
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.service.file.FileService
import org.glassfish.jersey.internal.LocalizationMessages
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import java.io.InputStream
import java.util.UUID
import javax.transaction.Transactional
import javax.ws.rs.ProcessingException

@Service
class ContainerService : BaseService() {

  companion object {
    private const val LABEL_PREFIX = "de.code-freak."
    const val LABEL_ANSWER_ID = LABEL_PREFIX + "answer-id"
    const val LABEL_LATEX_CONTAINER = "{$LABEL_PREFIX}latex-service"
    const val LABEL_INSTANCE_ID = LABEL_PREFIX + "instance-id"
    const val PROJECT_PATH = "/home/coder/project"
  }

  private val log = LoggerFactory.getLogger(this::class.java)
  private var idleContainers: Map<String, Long> = mapOf()

  @Autowired
  lateinit var docker: DockerClient

  @Autowired
  lateinit var config: AppConfiguration

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  @Autowired
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var answerService: AnswerService

  /**
   * Pull all required docker images on startup
   */
  @EventListener(ContextRefreshedEvent::class)
  fun pullDockerImages() {
    val images = listOf(config.ide.image, config.latex.image, config.evaluation.codeclimate.image)
    for (image in images) {
      val imageInfo = try {
        docker.inspectImage(image)
      } catch (e: ImageNotFoundException) {
        null
      }

      val pullRequired = config.docker.pullPolicy == "always" || (config.docker.pullPolicy == "if-not-present" && imageInfo == null)
      if (!pullRequired) {
        if (imageInfo == null) {
          log.warn("Image pulling is disabled but $image is not available on the daemon!")
        } else {
          log.info("Image present: $image ${imageInfo.id()}")
        }
        continue
      }

      log.info("Pulling latest image for: $image")
      docker.pull(image)
      log.info("Updated docker image $image to ${docker.inspectImage(image).id()}")
    }
  }

  @Synchronized
  fun getOrCreateLatexContainer(): String {
    var containerId = getContainerWithLabel(LABEL_LATEX_CONTAINER, "true")
    if (containerId !== null) {
      return containerId
    }

    containerId = createContainer(config.latex.image) {
      labels = mapOf(LABEL_LATEX_CONTAINER to "true")
      hostConfig { restartPolicy(HostConfig.RestartPolicy.unlessStopped()) }
      doNothingAndKeepAlive()
    }

    docker.startContainer(containerId)
    return containerId
  }

  /**
   * Convert the latex file in the given archive to pdf and return the directory after pdflatex has been run
   */
  fun latexConvert(inputTar: InputStream, file: String): InputStream {
    val latexContainerId = getOrCreateLatexContainer()
    val jobPath = exec(latexContainerId, arrayOf("mktemp", "-d")).trim()
    docker.copyToContainer(inputTar, latexContainerId, jobPath)
    exec(latexContainerId, arrayOf("sh", "-c", "cd $jobPath && xelatex -synctex=1 -interaction=nonstopmode $file"))
    val out = docker.archiveContainer(latexContainerId, "$jobPath/.")
    exec(latexContainerId, arrayOf("rm", "-rf", jobPath))
    return out
  }

  /**
   * Start an IDE container for the given submission and returns the container ID
   * If there is already a container for the submission it will be used instead
   */
  @Synchronized
  @Throws(ResourceLimitException::class)
  fun startIdeContainer(answer: Answer) {
    // either take existing container or create a new one
    var containerId = this.getIdeContainer(answer)
    if (containerId != null && isContainerRunning(containerId)) {
      return
    }

    if (!canStartNewIdeContainer()) {
      throw ResourceLimitException("Cannot start new IDE. Maximum capacity reached.")
    }

    if (containerId == null) {
      containerId = this.createIdeContainer(answer)
      docker.startContainer(containerId)
      // prepare the environment after the container has started
      this.copyFilesToIde(containerId, answer.id)
    } else {
      // make sure the container is running. Also existing ones could have been stopped
      docker.startContainer(containerId)
    }
  }

  fun canStartNewIdeContainer(): Boolean = config.ide.maxContainers < 0 ||
      getContainersWithLabel(LABEL_ANSWER_ID).size < config.ide.maxContainers

  /**
   * Run a command as root inside container and return the result as string
   */
  fun exec(containerId: String, cmd: Array<String>): String {
    val exec = docker.execCreate(
        containerId, cmd,
        DockerClient.ExecCreateParam.attachStdin(), // this is not needed but a workaround for spotify/docker-client#513
        DockerClient.ExecCreateParam.attachStdout(),
        DockerClient.ExecCreateParam.attachStderr(),
        DockerClient.ExecCreateParam.user("root")
    )
    val output = docker.execStart(exec.id())
    return output.readFully()
  }

  /**
   * Get the URL for an IDE container
   * TODO: make this configurable for different types of hosting/reverse proxies/etc
   */
  fun getIdeUrl(answerId: UUID): String {
    return "${config.traefik.url}/ide/$answerId/"
  }

  fun isIdeContainerRunning(answerId: UUID): Boolean {
    return getIdeContainer(answerId)?.let { isContainerRunning(it) } ?: false
  }

  protected fun getIdeContainer(answer: Answer): String? {
    return getIdeContainer(answer.id)
  }

  protected fun getIdeContainer(answerId: UUID): String? {
    return getContainerWithLabel(LABEL_ANSWER_ID, answerId.toString())
  }

  protected fun getContainerWithLabel(label: String, value: String? = null): String? {
    return getContainersWithLabel(label, value).firstOrNull()
  }

  protected fun getContainersWithLabel(label: String, value: String? = null): List<String> {
    return docker.listContainers(
        DockerClient.ListContainersParam.withLabel(label, value),
        DockerClient.ListContainersParam.withLabel(LABEL_INSTANCE_ID, config.instanceId)
    ).map { it.id() }
  }

  @Transactional
  fun saveAnswerFiles(answer: Answer): Answer {
    val containerId = getIdeContainer(answer.id) ?: return answer
    try {
      docker.archiveContainer(containerId, "$PROJECT_PATH/.").use { tar ->
        fileService.writeCollectionTar(answer.id).use { StreamUtils.copy(tar, it) }
      }
    } catch (e: ProcessingException) {
      // okay until this is fixed https://github.com/eclipse-ee4j/jersey/issues/3486
      if (e.message != LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED()) {
        throw e
      }
    }
    log.info("Saved files of container with id: $containerId")
    return entityManager.merge(answer)
  }

  protected fun createContainer(
    image: String,
    configure: ContainerBuilder.() -> Unit = {}
  ): String {

    val builder = ContainerBuilder()
    builder.configure()

    builder.containerConfig {
      image(image)
    }
    builder.labels += mapOf(
        LABEL_INSTANCE_ID to config.instanceId
    )

    return docker.createContainer(builder.build(), builder.name).id()!!
  }

  /**
   * Configure and create a new IDE container.
   * Returns the ID of the created container
   */
  protected fun createIdeContainer(answer: Answer): String {
    val answerId = answer.id.toString()

    val containerId = createContainer(config.ide.image) {
      labels = mapOf(
          LABEL_ANSWER_ID to answerId,
          "traefik.enable" to "true",
          "traefik.frontend.rule" to "PathPrefixStrip: /ide/$answerId/",
          "traefik.port" to "3000",
          "traefik.frontend.headers.customResponseHeaders" to "Access-Control-Allow-Origin:*"
      )
      hostConfig {
        restartPolicy(HostConfig.RestartPolicy.unlessStopped())
        capAdd("SYS_PTRACE") // required for lsof
        memory(config.ide.memory)
        memorySwap(config.ide.memory) // memory+swap = memory ==> 0 swap
        nanoCpus(config.ide.cpus * 1000000000L)
      }
    }

    // attach to network
    docker.connectToNetwork(containerId, config.ide.network)

    return containerId
  }

  /**
   * Prepare a running container with files and other commands like chmod, etc.
   */
  protected fun copyFilesToIde(containerId: String, answerId: UUID) {
    // extract possible existing files of the current submission into project dir
    if (fileService.collectionExists(answerId)) {
      fileService.readCollectionTar(answerId).use { docker.copyToContainer(it, containerId, PROJECT_PATH) }
    }

    // change owner from root to coder so we can edit our project files
    exec(containerId, arrayOf("chown", "-R", "coder:coder", PROJECT_PATH))
  }

  fun answerFilesUpdated(answerId: UUID) {
    getIdeContainer(answerId)?.let {
      // use sh to make globbing work
      // two globs: one for regular files and one for hidden files/dirs except . and ..
      exec(it, arrayOf("sh", "-c", "rm -rf $PROJECT_PATH/* $PROJECT_PATH/.[!.]*"))
      copyFilesToIde(it, answerId)
    }
  }

  protected fun isContainerRunning(containerId: String): Boolean = docker.inspectContainer(containerId).state().running()

  @Scheduled(
      fixedRateString = "\${code-freak.ide.idle-check-rate}",
      initialDelayString = "\${code-freak.ide.idle-check-rate}"
  )
  protected fun shutdownIdleIdeContainers() {
    log.debug("Checking for idle containers")
    // create a new map to not leak memory if containers disappear in another way
    val newIdleContainers: MutableMap<String, Long> = mutableMapOf()
    docker.listContainers(
        DockerClient.ListContainersParam.withLabel(LABEL_ANSWER_ID),
        DockerClient.ListContainersParam.withStatusRunning()
    )
        .forEach {
          val containerId = it.id()
          // TODO: Use `cat /proc/net/tcp` instead of lsof (requires no privileges)
          val connections = exec(containerId, arrayOf("/opt/code-freak/num-active-connections.sh")).trim()
          if (connections == "0") {
            val now = System.currentTimeMillis()
            val idleSince = idleContainers[containerId] ?: now
            val idleFor = now - idleSince
            log.debug("Container $containerId has been idle for more than $idleFor ms")
            if (idleFor >= config.ide.idleShutdownThreshold) {
              val answerId = it.labels()!![LABEL_ANSWER_ID]
              val answer = answerRepository.findById(UUID.fromString(answerId))
              if (answer.isPresent) {
                containerService.saveAnswerFiles(answer.get())
              } else {
                log.warn("Answer $answerId not found. Files are not saved!")
              }
              log.info("Shutting down container $containerId of answer $answerId")
              docker.stopContainer(containerId, 5)
            } else {
              newIdleContainers[containerId] = idleSince
            }
          }
        }
    idleContainers = newIdleContainers
  }

  fun runCodeclimate(answer: Answer): String {
    val containerId = createContainer(config.evaluation.codeclimate.image) {
      name = "codeclimate_orchestrator_${answer.id}"
      doNothingAndKeepAlive()
      hostConfig {
        appendBinds("/var/run/docker.sock:/var/run/docker.sock", "/tmp/cc:/tmp/cc")
      }
      containerConfig {
        env("CODECLIMATE_ORCHESTRATOR=$name", "CODECLIMATE_CODE=/code")
      }
    }
    answerService.copyFilesForEvaluation(answer).use { docker.copyToContainer(it, containerId, "/code") }
    docker.startContainer(containerId)
    // `analyze` would also install missing engines but may time out in the process. Also `engines:install` will update images.
    exec(containerId, arrayOf("/usr/src/app/bin/codeclimate", "engines:install"))
    val output = exec(containerId, arrayOf("/usr/src/app/bin/codeclimate", "analyze", "-f", "json"))
    docker.killContainer(containerId)
    docker.removeContainer(containerId)
    return output
  }
}
