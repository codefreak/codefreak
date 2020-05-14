package org.codefreak.codefreak.service

import com.spotify.docker.client.messages.ContainerInfo

interface ReverseProxy {
  fun configureContainer(containerBuilder: ContainerBuilder)
  fun getIdeUrl(containerInfo: ContainerInfo): String
}
