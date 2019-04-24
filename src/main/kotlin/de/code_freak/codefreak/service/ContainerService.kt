package de.code_freak.codefreak.service

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import de.code_freak.codefreak.entity.Answer
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import javax.transaction.Transactional
import kotlin.random.Random

@Service
class ContainerService(
  @Autowired
  val docker: DockerClient
) : BaseService() {
  companion object {
    const val IDE_DOCKER_IMAGE = "theiaide/theia-full:next"
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
    log.info("Finished pulling images")
  }

  /**
   * Start an IDE container for the given submission and returns the container ID
   * If there is already a container for the submission it will be used instead
   */
  fun startIdeContainer(answer: Answer): String {
    // either take existing container or create a new one
    val containerId = this.getIdeContainer(answer) ?: this.createIdeContainer(answer)
    // make sure the container is running. Also existing ones could have been stopped
    if (!isContainerRunning(containerId)) {
      docker.startContainer(containerId)
    }
    // prepare the environment after the container has started
    this.prepareIdeContainer(containerId, answer)

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
  protected fun getIdeContainer(answer: Answer): String? {
    return docker.listContainers(
        DockerClient.ListContainersParam.withLabel(LABEL_TASK_SUBMISSION_ID, answer.id.toString()),
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
    val id = answer.id.toString()

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
  protected fun prepareIdeContainer(containerId: String, answer: Answer) {
    // extract possible existing files of the current submission into /home/project
    answer.files?.let {
      docker.copyToContainer(it.inputStream(), containerId, "/home/project")
    }
  }

  protected fun isContainerRunning(containerId: String): Boolean =
      docker.inspectContainer(containerId).state().running()
}
