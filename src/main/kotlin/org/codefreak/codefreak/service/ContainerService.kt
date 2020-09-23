package org.codefreak.codefreak.service

import com.google.common.collect.MapMaker
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.ListContainersParam.allContainers
import com.spotify.docker.client.DockerClient.ListContainersParam.withLabel
import com.spotify.docker.client.DockerClient.ListContainersParam.withStatusExited
import com.spotify.docker.client.DockerClient.ListContainersParam.withStatusRunning
import com.spotify.docker.client.DockerClient.RemoveContainerParam.forceKill
import com.spotify.docker.client.DockerClient.RemoveContainerParam.removeVolumes
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.exceptions.ImageNotFoundException
import com.spotify.docker.client.messages.Container
import com.spotify.docker.client.messages.HostConfig
import java.io.InputStream
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import javax.ws.rs.ProcessingException
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.repository.AnswerRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.withTrailingSlash
import org.glassfish.jersey.internal.LocalizationMessages
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StreamUtils

@Service
class ContainerService : BaseService() {

  companion object {
    const val LABEL_PREFIX = "org.codefreak."
    const val LABEL_READ_ONLY_ANSWER_ID = LABEL_PREFIX + "answer-id-read-only"
    const val LABEL_ANSWER_ID = LABEL_PREFIX + "answer-id"
    const val LABEL_TASK_ID = LABEL_PREFIX + "task-id"
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
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  @Autowired
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var answerService: AnswerService

  @Autowired
  private lateinit var reverseProxy: ReverseProxy

  /**
   * A map that keeps a lock for each answer (collection) that should be used for file operations
   * @see withCollectionFileLock
   */
  private val answerFileLockMap = MapMaker().weakValues().makeMap<UUID, ReentrantLock>()

  /**
   * Inherit behaviour of the standard Docker CLI and fallback to :latest if no tag is given
   */
  fun normalizeImageName(imageName: String) =
      if (imageName.contains(':')) {
        imageName
      } else {
        "$imageName:latest"
      }

  fun pullDockerImage(image: String) {
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
        log.debug("Image present: $image ${imageInfo.id()}")
      }
      return
    }

    log.info("Pulling latest image for: $image")
    docker.pull(image)
    log.info("Updated docker image $image to ${docker.inspectImage(image).id()}")
  }

  /**
   * Start an IDE container for the given submission and returns the container ID
   * If there is already a container for the submission it will be used instead
   */
  @Throws(ResourceLimitException::class)
  fun startIdeContainer(answer: Answer, readOnly: Boolean = false): String {
    return startIdeContainer(answer.id, if (readOnly) LABEL_READ_ONLY_ANSWER_ID else LABEL_ANSWER_ID, answer.id)
  }

  @Throws(ResourceLimitException::class)
  fun startIdeContainer(task: Task): String {
    return startIdeContainer(task.id, LABEL_TASK_ID, task.id)
  }

  @Synchronized
  @Throws(ResourceLimitException::class)
  fun startIdeContainer(id: UUID, label: String, fileCollectionId: UUID): String {
    // either take existing container or create a new one
    val container = getContainerWithLabel(label, id.toString())
    if (container != null && isContainerRunning(container.id())) {
      return getIdeUrl(container.id())
    }

    if (!canStartNewIdeContainer()) {
      throw ResourceLimitException("Cannot start new IDE. Maximum capacity reached.")
    }

    // lock early so the container files will not be altered before it is ready
    return withCollectionFileLock(id) {
      if (container == null) {
        log.info("Creating new IDE container with $label=$id")
        val containerId = this.createIdeContainer(label, id)
        docker.startContainer(containerId)
        // prepare the environment after the container has started
        this.copyFilesToIde(containerId, fileCollectionId)
        getIdeUrl(containerId)
      } else {
        // make sure the container is running. Also existing ones could have been stopped
        docker.startContainer(container.id())
        // write fresh files that might have been uploaded while being stopped
        this.copyFilesToIde(container.id(), fileCollectionId)
        getIdeUrl(container.id())
      }
    }
  }

  fun canStartNewIdeContainer(): Boolean {
    if (config.ide.maxContainers < 0) return true
    return getAllIdeContainers(withStatusRunning()).size < config.ide.maxContainers
  }

  /**
   * Run a command as root inside container and return the result as string
   */
  fun exec(containerId: String, cmd: Array<String>): ExecResult {
    val exec = docker.execCreate(
        containerId, cmd,
        DockerClient.ExecCreateParam.attachStdin(), // this is not needed but a workaround for spotify/docker-client#513
        DockerClient.ExecCreateParam.attachStdout(),
        DockerClient.ExecCreateParam.attachStderr(),
        DockerClient.ExecCreateParam.user("root")
    )
    return ExecResult(output = docker.execStart(exec.id()).readFully(),
        exitCode = docker.execInspect(exec.id()).exitCode() ?: -1)
  }

  /**
   * Get the URL for an IDE container
   */
  fun getIdeUrl(containerId: String): String {
    return this.reverseProxy.getIdeUrl(
        docker.inspectContainer(containerId)
    )
  }

  fun isIdeContainerRunning(answerId: UUID): Boolean {
    return getAnswerIdeContainer(answerId)?.let { isContainerRunning(it) } ?: false
  }

  protected fun getAnswerIdeContainer(answerId: UUID, readOnly: Boolean = false): String? {
    val label = if (readOnly) LABEL_READ_ONLY_ANSWER_ID else LABEL_ANSWER_ID
    return getContainerWithLabel(label, answerId.toString())?.id()
  }

  protected fun getContainerWithLabel(label: String, value: String? = null): Container? {
    return getContainersWithLabel(label, value).firstOrNull()
  }

  protected fun getContainersWithLabel(label: String, value: String? = null) = listContainers(
      withLabel(label, value),
      allContainers()
  )

  protected fun listContainers(vararg listContainerParams: DockerClient.ListContainersParam): List<Container> {
    return docker.listContainers(withLabel(LABEL_INSTANCE_ID, config.instanceId), *listContainerParams)
  }

  @Transactional
  fun saveAnswerFiles(answer: Answer, force: Boolean = false): Answer {
    if (!force && !answer.isEditable) {
      log.info("Skipped saving of files from answer ${answer.id} because it's not editable anymore")
      return answer
    }
    val containerId = getAnswerIdeContainer(answer.id)
    if (containerId === null) {
      log.debug("Not saving files of answer ${answer.id}: No write container found")
      return answer
    }
    if (!isContainerRunning(containerId)) {
      log.debug("Skipped saving of files from answer ${answer.id} because IDE is not running")
      return answer
    }
    withCollectionFileLock(answer.id) {
      archiveContainer(containerId, "$PROJECT_PATH/.") { tar ->
        fileService.writeCollectionTar(answer.id).use { StreamUtils.copy(tar, it) }
      }
    }
    log.info("Saved files of answer ${answer.id} from container $containerId (force=$force)")
    return entityManager.merge(answer)
  }

  @Transactional
  fun saveTaskFiles(task: Task): Task {
    val containerId = getContainerWithLabel(LABEL_TASK_ID, task.id.toString())?.id() ?: return task
    archiveContainer(containerId, "$PROJECT_PATH/.") { tar ->
      fileService.writeCollectionTar(task.id).use { StreamUtils.copy(tar, it) }
    }
    log.info("Saved files of task ${task.id} from container $containerId")
    return entityManager.merge(task)
  }

  protected fun archiveContainer(containerId: String, path: String, process: (InputStream) -> Unit) {
    try {
      docker.archiveContainer(containerId, path).use(process)
    } catch (e: ProcessingException) {
      // okay until this is fixed https://github.com/eclipse-ee4j/jersey/issues/3486
      if (e.message != LocalizationMessages.MESSAGE_CONTENT_INPUT_STREAM_CLOSE_FAILED()) {
        throw e
      }
    }
  }

  protected fun createContainer(
    image: String,
    configure: ContainerBuilder.() -> Unit = {}
  ): String {
    val normalizedImageName = normalizeImageName(image)
    pullDockerImage(normalizedImageName)

    val builder = ContainerBuilder()
    builder.configure()

    builder.containerConfig {
      image(normalizedImageName)
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
  protected fun createIdeContainer(label: String, id: UUID): String {
    val containerId = createContainer(config.ide.image) {
      labels = mapOf(
          label to id.toString()
      )
      reverseProxy.configureContainer(this)
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
   * Write a file collection to an IDE container. This can only be called
   * on a running container and will throw an IllegalStateException otherwise.
   * Please lock the collection before using this method to prevent losing files
   */
  protected fun copyFilesToIde(containerId: String, fileCollectionId: UUID) {
    // extract possible existing files of the current submission into project dir
    if (fileService.collectionExists(fileCollectionId)) {
      // remove existing files before writing new ones
      // use sh to make globbing work
      // two globs: one for regular files and one for hidden files/dirs except . and ..
      exec(containerId, arrayOf("sh", "-c", "rm -rf $PROJECT_PATH/* $PROJECT_PATH/.[!.]*"))

      fileService.readCollectionTar(fileCollectionId).use {
        docker.copyToContainer(it, containerId, PROJECT_PATH)
      }
    }

    // change owner from root to coder so we can edit our project files
    exec(containerId, arrayOf("chown", "-R", "coder:coder", PROJECT_PATH))
  }

  fun answerFilesUpdated(answerId: UUID) {
    try {
      withCollectionFileLock(answerId) {
        getAnswerIdeContainer(answerId)?.let { copyFilesToIde(it, answerId) }
      }
    } catch (e: IllegalStateException) {
      // happens if the IDE is not running.
      // We could check if it is running before but the container might get killed while we update files
      log.debug("Not updating files in IDE for answer $answerId: ${e.message}")
    }
  }

  protected fun isContainerRunning(containerId: String): Boolean = try {
    docker.inspectContainer(containerId).state().running()
  } catch (e: ContainerNotFoundException) {
    false
  }

  protected fun getAllIdeContainers(
    vararg listParams: DockerClient.ListContainersParam
  ) = mutableListOf<Container>().apply {
    addAll(listContainers(withLabel(LABEL_ANSWER_ID), *listParams))
    addAll(listContainers(withLabel(LABEL_READ_ONLY_ANSWER_ID), *listParams))
    addAll(listContainers(withLabel(LABEL_TASK_ID), *listParams))
  }

  @Scheduled(
      fixedRateString = "\${codefreak.ide.idle-check-rate}",
      initialDelayString = "\${codefreak.ide.idle-check-rate}"
  )
  protected fun shutdownIdleIdeContainers() {
    log.debug("Checking for idle containers")
    // create a new map to not leak memory if containers disappear in another way
    val newIdleContainers: MutableMap<String, Long> = mutableMapOf()
    getAllIdeContainers(withStatusRunning()).forEach {
      val containerId = it.id()
      // TODO: Use `cat /proc/net/tcp` instead of lsof (requires no privileges)
      val connections = exec(containerId, arrayOf("/opt/code-freak/num-active-connections.sh")).output.trim()
      if (connections == "0") {
        val now: Long = System.currentTimeMillis()
        val idleSince: Long = idleContainers[containerId] ?: now
        val idleFor = now - idleSince
        log.debug("Container $containerId has been idle for more than $idleFor ms")
        if (idleFor >= config.ide.idleShutdownThreshold) {
          val labels = it.labels()!!
          when {
            labels.containsKey(LABEL_READ_ONLY_ANSWER_ID)
            -> log.info("Shutting down read container $containerId for answer ${labels[LABEL_READ_ONLY_ANSWER_ID]}")
            labels.containsKey(LABEL_ANSWER_ID) -> {
              val answer = answerRepository.findById(UUID.fromString(labels[LABEL_ANSWER_ID]))
              if (answer.isPresent) {
                containerService.saveAnswerFiles(answer.get())
              } else {
                log.warn("Answer ${labels[LABEL_ANSWER_ID]} not found. Files are not saved!")
              }
              log.info("Shutting down container $containerId of answer ${labels[LABEL_ANSWER_ID]}")
            }
            labels.containsKey(LABEL_TASK_ID) -> {
              val task = taskRepository.findById(UUID.fromString(labels[LABEL_TASK_ID]))
              if (task.isPresent) {
                containerService.saveTaskFiles(task.get())
              } else {
                log.warn("Task ${labels[LABEL_TASK_ID]} not found. Files are not saved!")
              }
              log.info("Shutting down container $containerId of task ${labels[LABEL_TASK_ID]}")
            }
          }
          docker.stopContainer(containerId, 5)
        } else {
          newIdleContainers[containerId] = idleSince
        }
      }
    }
    idleContainers = newIdleContainers
  }

  @Scheduled(
      fixedRateString = "#{@config.ide.removeCheckRate}",
      initialDelayString = "#{@config.ide.removeCheckRate}"
  )
  protected fun removeShutdownContainers() {
    val thresholdDate = Date.from(Instant.now().minusMillis(config.ide.removeThreshold))
    log.debug("Removing IDE containers exited before $thresholdDate")
    getAllIdeContainers(withStatusExited()).forEach { container ->
      val containerId = container.id()
      val inspection = docker.inspectContainer(containerId)
      if (inspection.state().finishedAt().before(thresholdDate)) {
        val entityId = inspection.config().labels()?.let {
          it[LABEL_ANSWER_ID] ?: it[LABEL_READ_ONLY_ANSWER_ID] ?: it[LABEL_TASK_ID]
        }
        log.info("Removing container $containerId of entity $entityId")
        docker.removeContainer(containerId, forceKill(), removeVolumes())
      }
    }
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
    useContainer(containerId) {
      answerService.copyFilesForEvaluation(answer).use { docker.copyToContainer(it, containerId, "/code") }
      // `analyze` would also install missing engines but may time out in the process. Also `engines:install` will update images.
      exec(containerId, arrayOf("/usr/src/app/bin/codeclimate", "engines:install"))
      return exec(containerId, arrayOf("/usr/src/app/bin/codeclimate", "analyze", "-f", "json")).output
    }
  }

  fun runCommandsForEvaluation(
    answer: Answer,
    image: String,
    projectPath: String,
    commands: List<String>,
    stopOnFail: Boolean,
    processFiles: ((InputStream) -> Unit)? = null
  ): List<ExecResult> {
    val containerId = createContainer(image) {
      doNothingAndKeepAlive()
      containerConfig { workingDir(projectPath) }
    }
    val outputs = mutableListOf<ExecResult>()
    useContainer(containerId) {
      answerService.copyFilesForEvaluation(answer).use { docker.copyToContainer(it, containerId, projectPath) }
      commands.forEach {
        if (stopOnFail && outputs.size > 0 && outputs.last().exitCode != 0L) {
          outputs.add(ExecResult("", -1))
        } else {
          outputs.add(exec(containerId, splitCommand(it)))
        }
      }
      if (processFiles !== null) {
        archiveContainer(containerId, "${projectPath.withTrailingSlash()}.", processFiles)
      }
      return outputs
    }
  }

  /**
   * Start a container and runs the specified block.
   * Ensures container is removed after execution with all attached volumes
   */
  private inline fun <T> useContainer(containerId: String, block: () -> T): T {
    docker.startContainer(containerId)
    try {
      return block()
    } finally {
      docker.removeContainer(containerId, forceKill(), removeVolumes())
    }
  }

  private fun splitCommand(command: String): Array<String> {
    // from https://stackoverflow.com/a/366532/5519485
    val matchList = ArrayList<String>()
    val regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")
    val regexMatcher = regex.matcher(command)
    while (regexMatcher.find()) {
      when {
        regexMatcher.group(1) != null -> // Add double-quoted string without the quotes
          matchList.add(regexMatcher.group(1))
        regexMatcher.group(2) != null -> // Add single-quoted string without the quotes
          matchList.add(regexMatcher.group(2))
        else -> // Add unquoted word
          matchList.add(regexMatcher.group())
      }
    }
    return matchList.toArray(arrayOf())
  }

  /**
   * Lock collection for file operations (read/write files in the container)
   * We cannot use the container id for locking because we need to lock even before we know the container id
   */
  fun <T> withCollectionFileLock(collectionId: UUID, block: () -> T): T {
    val lock = answerFileLockMap.getOrPut(collectionId) { ReentrantLock() }
    lock.lock()
    try {
      return block()
    } finally {
      lock.unlock()
    }
  }
}
