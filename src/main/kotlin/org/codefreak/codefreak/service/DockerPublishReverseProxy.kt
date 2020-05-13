package org.codefreak.codefreak.service

import com.spotify.docker.client.messages.ContainerInfo
import com.spotify.docker.client.messages.PortBinding

class DockerPublishReverseProxy(
  private val baseUrl: String,
  private val idePort: String
) : ReverseProxy {
  override fun configureContainer(containerBuilder: ContainerBuilder) {
    containerBuilder.hostConfig {
      portBindings(mapOf(
          "$idePort/tcp" to listOf(PortBinding.randomPort("0.0.0.0"))
      ))
    }
    containerBuilder.containerConfig {
      exposedPorts("$idePort/tcp")
    }
  }

  override fun getIdeUrl(containerInfo: ContainerInfo): String {
    val port = containerInfo.networkSettings()?.ports()?.get("$idePort/tcp")?.first() ?: throw RuntimeException(
        "Container ${containerInfo.id()} does not expose port $idePort/tcp. IDE is unreachable."
    )
    return "$baseUrl:${port.hostPort()}"
  }
}
