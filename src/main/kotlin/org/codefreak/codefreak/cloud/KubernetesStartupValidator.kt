package org.codefreak.codefreak.cloud

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KubernetesStartupValidator : InitializingBean {
  companion object {
    private val log = LoggerFactory.getLogger(KubernetesStartupValidator::class.java)
  }

  @Autowired
  lateinit var client: KubernetesClient

  override fun afterPropertiesSet() {
    try {
      log.info("Connected to Kubernetes Cluster ${client.masterUrl} running Kubernetes ${client.version.gitVersion}")
    } catch (e: KubernetesClientException) {
      log.error("Cannot connect to Kubernetes API running at ${client.masterUrl}:", e)
    }
  }
}
