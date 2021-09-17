package org.codefreak.codefreak.cloud.model

import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newServicePort
import com.fkorotkov.kubernetes.spec
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Service
import org.codefreak.codefreak.cloud.KubernetesWorkspaceConfig

class CompanionService(wsConfig: KubernetesWorkspaceConfig) : Service() {
  init {
    metadata {
      name = wsConfig.companionServiceName
      labels = wsConfig.getLabelsForComponent("companion")
    }
    spec {
      ports = listOf(newServicePort {
        name = "http"
        port = 80
        targetPort = IntOrString("http")
      })
      type = "ClusterIP"
      selector = wsConfig.getLabelsForComponent("companion")
    }
  }
}
