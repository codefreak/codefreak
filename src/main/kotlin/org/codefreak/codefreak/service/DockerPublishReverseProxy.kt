package org.codefreak.codefreak.service

import com.spotify.docker.client.messages.ContainerInfo
import com.spotify.docker.client.messages.PortBinding

class DockerPublishReverseProxy : ReverseProxy {
  override fun configureContainer(containerBuilder: ContainerBuilder) {
    containerBuilder.hostConfig {
      portBindings(mapOf(
          "3000/tcp" to listOf(PortBinding.randomPort("0.0.0.0"))
      ))
    }
    containerBuilder.containerConfig {
      exposedPorts("3000/tcp")
    }
  }

  override fun getIdeUrl(containerInfo: ContainerInfo): String {
    val port = containerInfo.networkSettings()?.ports()?.get("3000/tcp")?.first() ?: throw RuntimeException(
        "Container ${containerInfo.id()} does not expose port 3000/tcp. IDE is unreachable."
    )
    return "http://localhost:${port.hostPort()}"
  }
}
