package de.code_freak.codefreak.config

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DockerConfiguration {
  @Bean(destroyMethod = "close")
  fun dockerClient(
    @Value("\${code-freak.docker.host:unix:///var/run/docker.sock}") uri: String
  ): DockerClient {
    // TODO: Pull required docker images during boot
    return DefaultDockerClient.builder()
        .uri(uri)
        .build()
  }
}
