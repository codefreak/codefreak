package org.codefreak.codefreak.service

import com.spotify.docker.client.messages.ContainerInfo
import org.apache.commons.lang.RandomStringUtils
import java.security.SecureRandom
import java.util.Random

class TraefikReverseProxy(
  private val url: String,
  private val idePort: String,
  private val random: Random = SecureRandom()
) : ReverseProxy {
  companion object {
    const val LABEL_TOKEN = ContainerService.LABEL_PREFIX + "traefik.token"
  }

  override fun configureContainer(containerBuilder: ContainerBuilder) {
    val token = generateRandomToken()
    containerBuilder.labels = containerBuilder.labels.toMutableMap().apply {
      putAll(mapOf(
          LABEL_TOKEN to token,
          "traefik.enable" to "true",
          "traefik.frontend.rule" to "PathPrefixStrip: " + getIdePath(token),
          "traefik.port" to idePort
      ))
    }
  }

  override fun getIdeUrl(containerInfo: ContainerInfo): String {
    val token: String = containerInfo.config().labels()?.get(LABEL_TOKEN) ?: throw RuntimeException(
        "Container ${containerInfo.id()} misses $LABEL_TOKEN label"
    )
    return url + getIdePath(token)
  }

  private fun getIdePath(token: String) = "/ide/$token/"

  private fun generateRandomToken(): String {
    return RandomStringUtils.random(40, 0, 0, true, true, null, random)
  }
}
