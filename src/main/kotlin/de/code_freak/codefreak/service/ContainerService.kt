package de.code_freak.codefreak.service

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import de.code_freak.codefreak.entity.Answer
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.util.UUID
import javax.transaction.Transactional

@Service
class ContainerService(
  @Autowired
  val docker: DockerClient
) : BaseService() {
  companion object {
    const val IDE_DOCKER_IMAGE = "cfreak/theia:latest"
    val DOCKER_IMAGES = listOf(
        IDE_DOCKER_IMAGE
    )
    private const val LABEL_PREFIX = "de.code-freak."
    const val LABEL_ANSWER_ID = LABEL_PREFIX + "answer-id"
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Pull all required docker images on startup
   */
  @EventListener(ApplicationReadyEvent::class)
  fun pullDockerImages() {
    log.info("Pulling latest image for: " + DOCKER_IMAGES.joinToString())
    DOCKER_IMAGES.parallelStream().forEach {
      docker.pull(it)
    }
    log.info("Finished pulling images")
  }

  /**
   * Start an IDE container for the given submission and returns the container ID
   * If there is already a container for the submission it will be used instead
   */
  fun startIdeContainer(answer: Answer) {
    // either take existing container or create a new one
    val containerId = this.getIdeContainer(answer) ?: this.createIdeContainer(answer)
    // make sure the container is running. Also existing ones could have been stopped
    if (!isContainerRunning(containerId)) {
      docker.startContainer(containerId)
    }
    // prepare the environment after the container has started
    this.prepareIdeContainer(containerId, answer)
  }

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
    return "http://localhost:8081/ide/$answerId/"
  }

  /**
   * Try to find an existing container for the given submission
   */
  protected fun getIdeContainer(answer: Answer): String? {
    return docker.listContainers(
        DockerClient.ListContainersParam.withLabel(LABEL_ANSWER_ID, answer.id.toString()),
        DockerClient.ListContainersParam.limitContainers(1)
    ).firstOrNull()?.id()
  }

  @Transactional
  fun saveAnswerFiles(answer: Answer) {
    val containerId = getIdeContainer(answer) ?: throw IllegalArgumentException()
    val files = docker.archiveContainer(containerId, "/home/project/.")
    answer.files = IOUtils.toByteArray(files)
    entityManager.merge(answer)
    log.info("Saved files of container with id: $containerId")
  }

  /**
   * Configure and create a new IDE container.
   * Returns the ID of the created container
   */
  protected fun createIdeContainer(answer: Answer): String {
    val answerId = answer.id.toString()

    val labels = mapOf(
        LABEL_ANSWER_ID to answerId,
        "traefik.enable" to "true",
        "traefik.frontend.rule" to "PathPrefixStrip: /ide/$answerId/",
        "traefik.port" to "3000",
        "traefik.frontend.headers.customResponseHeaders" to "Access-Control-Allow-Origin:*"
    )

    val hostConfig = HostConfig.builder().build()

    val containerConfig = ContainerConfig.builder()
        .image(IDE_DOCKER_IMAGE)
        .labels(labels)
        .hostConfig(hostConfig)
        .build()

    val container = docker.createContainer(containerConfig)
    return container.id()!!
  }

  /**
   * Prepare a running container with files and other commands like chmod, etc.
   */
  protected fun prepareIdeContainer(containerId: String, answer: Answer) {
    // extract possible existing files of the current submission into /home/project
    answer.files?.let {
      docker.copyToContainer(it.inputStream(), containerId, "/home/project")
    }
    // change owner from root to theia so we can edit our project files
    exec(containerId, arrayOf("chown", "-R", "theia:theia", "/home/project"))
  }

  protected fun isContainerRunning(containerId: String): Boolean =
      docker.inspectContainer(containerId).state().running()
}
