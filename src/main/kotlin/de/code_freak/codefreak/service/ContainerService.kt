package de.code_freak.codefreak.service

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.ContainerConfig
import org.springframework.stereotype.Service

@Service
class ContainerService {
  fun startIdeContainer() {
    val docker = DefaultDockerClient.builder().uri("http://localhost:2375").build()

    val containerConfig = ContainerConfig.builder()
        .image("theiaide/theia")
        .build()

    val container = docker.createContainer(containerConfig)

    docker.startContainer(container.id())

    docker.close()
  }
}
