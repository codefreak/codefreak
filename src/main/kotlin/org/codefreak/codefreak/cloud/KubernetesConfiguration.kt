package org.codefreak.codefreak.cloud

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import org.codefreak.codefreak.config.AppConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KubernetesConfiguration {

  @Bean
  fun k8sClient(config: AppConfiguration): KubernetesClient {
    return DefaultKubernetesClient()
        .inNamespace(config.workspaces.namespace)
  }
}
