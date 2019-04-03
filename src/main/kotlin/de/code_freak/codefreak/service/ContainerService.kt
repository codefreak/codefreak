package de.code_freak.codefreak.service

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerInfo
import com.spotify.docker.client.messages.HostConfig
import com.spotify.docker.client.messages.PortBinding
import de.code_freak.codefreak.entity.SubmissionTask
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class ContainerService {
  final val DOCKER_IMAGE = "theiaide/theia:next"
  final val LABEL_PREFIX = "de.code-freak."
  final val LABEL_SUBMISSION_ID = LABEL_PREFIX + "submission-id"
  final val LABEL_THEIA_PORT = LABEL_PREFIX + "theia-port"

  @Autowired
  lateinit var docker: DockerClient

  /**
   * Start an IDE container for the given submission
   */
  fun startIdeContainer(submissionTask: SubmissionTask): ContainerInfo {
    // TODO: Pull only on boot and then periodically
    docker.pull(DOCKER_IMAGE)

    // TODO: find existing container with label set and make sure it is running before creating a new one
    val id = submissionTask.id.toString()

    // 49152-65535 is the private port range
    val theiaPort = Random.nextInt(49152, 65535).toString()

    val labelMap = mapOf(
        LABEL_SUBMISSION_ID to id,
        LABEL_THEIA_PORT to theiaPort
    )

    val publishedPorts = mapOf(
        "3000" to listOf(PortBinding.of("0.0.0.0", theiaPort))
    )
    val hostConfig = HostConfig.builder().portBindings(publishedPorts).build()

    val containerConfig = ContainerConfig.builder()
        .image(DOCKER_IMAGE)
        .labels(labelMap)
        .hostConfig(hostConfig)
        .exposedPorts(publishedPorts.keys)
        .build()

    val container = docker.createContainer(containerConfig)
    docker.startContainer(container.id())

    submissionTask.files?.let {
      docker.copyToContainer(it.inputStream(), container.id(), "/home/project")
    }

    return docker.inspectContainer(container.id())
  }

  fun getIdeUrl(containerInfo: ContainerInfo): String {
    val port = containerInfo.config().labels()!![LABEL_THEIA_PORT]

    return "http://localhost:$port"
  }
}
