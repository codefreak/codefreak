package org.codefreak.codefreak.service.workspace.model

import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newServicePort
import com.fkorotkov.kubernetes.spec
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.Service
import org.codefreak.codefreak.service.workspace.WorkspaceIdentifier
import org.codefreak.codefreak.service.workspace.k8sLabels
import org.codefreak.codefreak.service.workspace.workspaceServiceName

class WorkspaceServiceModel(identifier: WorkspaceIdentifier) : Service() {
  init {
    metadata {
      name = identifier.workspaceServiceName
      labels = identifier.k8sLabels
    }
    spec {
      ports = listOf(newServicePort {
        name = "http"
        port = 80
        targetPort = IntOrString("http")
      })
      type = "ClusterIP"
      selector = identifier.k8sLabels
    }
  }
}
