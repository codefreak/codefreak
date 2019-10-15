package de.code_freak.codefreak.service

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig

class ContainerBuilder {
  private val hostConfigBuilder = HostConfig.builder()
  private val containerConfigBuilder = ContainerConfig.builder()

  var labels: Map<String, String> = mapOf()
  var name: String? = null
  fun hostConfig(modify: HostConfig.Builder.() -> Unit) = hostConfigBuilder.modify()
  fun containerConfig(modify: ContainerConfig.Builder.() -> Unit) = containerConfigBuilder.modify()
  fun shellScript(script: String) = containerConfig {
    entrypoint("sh", "-c")
    cmd(script)
  }

  fun build(): ContainerConfig = containerConfigBuilder
      .hostConfig(hostConfigBuilder.build())
      .labels(labels)
      .build()
}
