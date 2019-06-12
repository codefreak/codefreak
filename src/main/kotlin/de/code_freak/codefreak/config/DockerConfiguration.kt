package de.code_freak.codefreak.config

import com.google.common.base.Optional
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerCertificates
import com.spotify.docker.client.DockerCertificatesStore
import com.spotify.docker.client.DockerClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
 * - code-freak.docker.host:            equal to DOCKER_HOST
 * - code-freak.docker.certPath:        equal to DOCKER_CERT_PATH
 * - code-freak.docker.caCertPath:      path to server certificate in .pem format
 * - code-freak.docker.clientKeyPath:   path to client key in .pem format
 * - code-freak.docker.clientCertPath:  path to client certificate in .pem format
 */
@Configuration
@ConfigurationProperties(prefix = "code-freak.docker")
class DockerConfiguration {
  lateinit var host: String

  lateinit var certPath: String

  lateinit var caCertPath: String

  lateinit var clientKeyPath: String

  lateinit var clientCertPath: String

  /**
   * Memory limit in bytes
   * Equal to --memory-swap in docker run
   * 0 means no limit
   */
  var memory = 0L

  /**
   * Number of CPUs per container
   * Equal to --cpus in docker run
   * 0 means no limit
   */
  var cpus = 0L

  /**
   * Name of the network the container will be attached to
   * Default is the "bridge" network (Docker default)
   */
  lateinit var network: String

  /**
   * Define how images will be pulled on application startup (inspired by Gitlab Runner)
   * - never = Images must be already present on the docker daemon or container creation will fail
   * - if-not-present = Pull images if no version is available
   * - always = Always pull image (may override existing ones)
   */
  lateinit var pullPolicy: String

  @Bean(destroyMethod = "close")
  fun dockerClient(): DockerClient {
    val builder = DefaultDockerClient.fromEnv()

    if (!host.isBlank()) {
      builder.uri(host)
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
    if (certPath.isBlank()) {
      return Optional.absent()
    }
    return DockerCertificates.Builder().dockerCertPath(Paths.get(certPath)).build()
  }

  /**
   * Get certificate store based on individual paths for ca cert, client cert and client key
   */
  private fun getCertificatesFromFiles(): Optional<DockerCertificatesStore> {
    if (caCertPath.isBlank() || clientCertPath.isBlank() || clientKeyPath.isBlank()) {
      return Optional.absent()
    }
    return DockerCertificates.Builder()
        .caCertPath(Paths.get(caCertPath))
        .clientCertPath(Paths.get(clientCertPath))
        .clientKeyPath(Paths.get(clientKeyPath))
        .build()
  }
}
