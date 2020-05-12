package org.codefreak.codefreak.config

import com.google.common.base.Optional
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerCertificates
import com.spotify.docker.client.DockerCertificatesStore
import com.spotify.docker.client.DockerClient
import org.codefreak.codefreak.service.DockerPublishReverseProxy
import org.codefreak.codefreak.service.ReverseProxy
import org.codefreak.codefreak.service.TraefikReverseProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.nio.file.Paths

/**
 * Configure the Docker client
 * The default behaviour is inherited from the Docker Spotify client:
 * https://github.com/spotify/docker-client/blob/master/docs/user_manual.md
 *
 * It respects the default docker environment variables
 * - DOCKER_HOST        e.g. localhost, http://docker-server:1234
 * - DOCKER_PORT        e.g. 2375
 * - DOCKER_CERT_PATH   e.g. /path/to/certs which contains ca.pem, cert.pem, key.pem
 *
 * Otherwise the configuration can be overridden with the following properties:
 * - codefreak.docker.host:            equal to DOCKER_HOST
 * - codefreak.docker.certPath:        equal to DOCKER_CERT_PATH
 * - codefreak.docker.caCertPath:      path to server certificate in .pem format
 * - codefreak.docker.clientKeyPath:   path to client key in .pem format
 * - codefreak.docker.clientCertPath:  path to client certificate in .pem format
 */
@Configuration
class DockerConfiguration {

  @Autowired
  lateinit var config: AppConfiguration

  @Bean(destroyMethod = "close")
  fun dockerClient(): DockerClient {
    val builder = DefaultDockerClient.fromEnv()

    if (!config.docker.host.isBlank()) {
      builder.uri(config.docker.host)
    }

    val certificatesStore = getCertificatesFromPath().or(getCertificatesFromFiles()).orNull()
    certificatesStore?.let(builder::dockerCertificates)

    return builder.build()
  }

  /**
   * Get a certificates store based on a single directory with the files:
   * - ca.pem
   * - cert.pem
   * - key.pem
   */
  private fun getCertificatesFromPath(): Optional<DockerCertificatesStore> {
    if (config.docker.certPath.isBlank()) {
      return Optional.absent()
    }
    return DockerCertificates.Builder().dockerCertPath(Paths.get(config.docker.certPath)).build()
  }

  /**
   * Get certificate store based on individual paths for ca cert, client cert and client key
   */
  private fun getCertificatesFromFiles(): Optional<DockerCertificatesStore> {
    if (config.docker.caCertPath.isBlank() || config.docker.clientCertPath.isBlank() || config.docker.clientKeyPath.isBlank()) {
      return Optional.absent()
    }
    return DockerCertificates.Builder()
        .caCertPath(Paths.get(config.docker.caCertPath))
        .clientCertPath(Paths.get(config.docker.clientCertPath))
        .clientKeyPath(Paths.get(config.docker.clientKeyPath))
        .build()
  }

  @Bean
  @ConditionalOnProperty("codefreak.reverse-proxy.type", matchIfMissing = true)
  @Primary
  fun defaultReverseProxy(): ReverseProxy {
    return DockerPublishReverseProxy()
  }

  @Bean
  @ConditionalOnProperty("codefreak.reverse-proxy.type", havingValue = "traefik")
  fun traefikReverseProxy(): ReverseProxy {
    return TraefikReverseProxy(config.reverseProxy.traefik.url)
  }
}
