package de.code_freak.codefreak.service

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import de.code_freak.codefreak.entity.TaskSubmission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class ContainerService(
  @Autowired
  val docker: DockerClient
) {
  companion object {
    const val IDE_DOCKER_IMAGE = "cfreak/theia:latest"
    val DOCKER_IMAGES = listOf(
        IDE_DOCKER_IMAGE
    )
    const val LABEL_PREFIX = "de.code-freak."
    const val LABEL_TASK_SUBMISSION_ID = LABEL_PREFIX + "task-submission-id"
    const val LABEL_THEIA_PORT = LABEL_PREFIX + "theia-port"
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
  }

  /**
   * Start an IDE container for the given submission and returns the container ID
   * If there is already a container for the submission it will be used instead
   */
  fun startIdeContainer(taskSubmission: TaskSubmission): String {
    // either take existing container or create a new one
    val containerId = this.getIdeContainer(taskSubmission) ?: this.createIdeContainer(taskSubmission)
    // make sure the container is running. Also existing ones could have been stopped
    if (!isContainerRunning(containerId)) {
      docker.startContainer(containerId)
    }
    // prepare the environment after the container has started
    this.prepareIdeContainer(containerId, taskSubmission)

    return containerId
  }

  /**
   * Get the URL for an IDE container
   * TODO: make this configurable for different types of hosting/reverse proxies/etc
   */
  fun getIdeUrl(containerId: String): String {
    val containerInfo = docker.inspectContainer(containerId)
    val port = containerInfo.config().labels()!![LABEL_THEIA_PORT]

    return "http://localhost:$port"
  }

  /**
   * Try to find an existing container for the given submission
   */
  protected fun getIdeContainer(taskSubmission: TaskSubmission): String? {
    return docker.listContainers(
        DockerClient.ListContainersParam.withLabel(LABEL_TASK_SUBMISSION_ID, taskSubmission.id.toString()),
        DockerClient.ListContainersParam.limitContainers(1)
    ).firstOrNull()?.id()
  }

  /**
   * Configure and create a new IDE container.
   * Returns the ID of the created container
   */
  protected fun createIdeContainer(taskSubmission: TaskSubmission): String {
    val id = taskSubmission.id.toString()

    // 49152-65535 is the private port range
    val theiaPort = Random.nextInt(49152, 65535).toString()

    val labelMap = mapOf(
        LABEL_TASK_SUBMISSION_ID to id,
        LABEL_THEIA_PORT to theiaPort
    )

    val publishedPorts = mapOf(
        "3000" to listOf(PortBinding.of("0.0.0.0", theiaPort))
    )
    val hostConfig = HostConfig.builder().portBindings(publishedPorts).build()

    val containerConfig = ContainerConfig.builder()
        .image(IDE_DOCKER_IMAGE)
        .labels(labelMap)
        .hostConfig(hostConfig)
        .exposedPorts(publishedPorts.keys)
        .build()

    val container = docker.createContainer(containerConfig)
    return container.id()!!
  }

  /**
   * Prepare a running container with files and other commands like chmod, etc.
   */
  protected fun prepareIdeContainer(containerId: String, taskSubmission: TaskSubmission) {
    // extract possible existing files of the current submission into /home/project
    taskSubmission.files?.let {
      docker.copyToContainer(it.inputStream(), containerId, "/home/project")
    }
  }

  protected fun isContainerRunning(containerId: String): Boolean =
      docker.inspectContainer(containerId).state().running()
}
