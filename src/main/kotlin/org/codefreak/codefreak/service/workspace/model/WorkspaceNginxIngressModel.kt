package org.codefreak.codefreak.service.workspace.model

import com.fkorotkov.kubernetes.networking.v1.backend
import com.fkorotkov.kubernetes.networking.v1.http
import com.fkorotkov.kubernetes.networking.v1.metadata
import com.fkorotkov.kubernetes.networking.v1.newHTTPIngressPath
import com.fkorotkov.kubernetes.networking.v1.newIngressRule
import com.fkorotkov.kubernetes.networking.v1.port
import com.fkorotkov.kubernetes.networking.v1.service
import com.fkorotkov.kubernetes.networking.v1.spec
import io.fabric8.kubernetes.api.model.networking.v1.Ingress
import java.net.URI
import org.codefreak.codefreak.service.workspace.WorkspaceIdentifier
import org.codefreak.codefreak.service.workspace.k8sLabels
import org.codefreak.codefreak.service.workspace.workspaceIngressName
import org.codefreak.codefreak.service.workspace.workspaceServiceName
import org.codefreak.codefreak.util.withoutTrailingSlash

class WorkspaceNginxIngressModel(identifier: WorkspaceIdentifier, baseUrl: URI) : Ingress() {
  init {
    metadata {
      name = identifier.workspaceIngressName
      labels = identifier.k8sLabels
      annotations = mapOf(
        "nginx.ingress.kubernetes.io/rewrite-target" to "/$2",
        "nginx.ingress.kubernetes.io/proxy-body-size" to "10m"
      )
    }
    spec {
      rules = listOf(newIngressRule {
        host = baseUrl.host
        http {
          paths = listOf(newHTTPIngressPath {
            pathType = "Prefix"
            path = "${baseUrl.path.withoutTrailingSlash()}(/|\$)(.*)"
            backend {
              service {
                name = identifier.workspaceServiceName
                port {
                  name = "http"
                }
              }
            }
          })
        }
      })
    }
  }
}
