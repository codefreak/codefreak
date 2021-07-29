package org.codefreak.codefreak.cloud.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fkorotkov.kubernetes.networking.v1.backend
import com.fkorotkov.kubernetes.networking.v1.http
import com.fkorotkov.kubernetes.networking.v1.metadata
import com.fkorotkov.kubernetes.networking.v1.newHTTPIngressPath
import com.fkorotkov.kubernetes.networking.v1.newIngressRule
import com.fkorotkov.kubernetes.networking.v1.port
import com.fkorotkov.kubernetes.networking.v1.service
import com.fkorotkov.kubernetes.networking.v1.spec
import io.fabric8.kubernetes.api.model.networking.v1.Ingress
import org.codefreak.codefreak.cloud.WorkspaceConfiguration
import org.codefreak.codefreak.util.withoutLeadingSlash
import org.codefreak.codefreak.util.withoutTrailingSlash

class CompanionIngress(private val wsConfig: WorkspaceConfiguration) : Ingress() {
  init {
    metadata {
      name = wsConfig.companionIngressName
      labels = wsConfig.getLabelsForComponent("companion")
      annotations = mapOf(
          "nginx.ingress.kubernetes.io/rewrite-target" to "/$2"
      )
    }
    spec {
      rules = listOf(newIngressRule {
        host = getHostName()
        http {
          paths = listOf(newHTTPIngressPath {
            pathType = "Prefix"
            path = "/${getBasePath()}(/|\$)(.*)"
            backend {
              service {
                name = wsConfig.companionServiceName
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

  private fun getBasePath(): String {
    val defaultPath = "ws-${wsConfig.workspaceId}"
    return if (wsConfig.baseUrl.path.isNotEmpty()) {
      "${wsConfig.baseUrl.path.withoutTrailingSlash().withoutLeadingSlash()}/$defaultPath"
    } else {
      defaultPath
    }
  }

  private fun getHostName(): String {
    return wsConfig.baseUrl.host
  }

  @JsonIgnore
  fun getBaseUrl(): String {
    return "${wsConfig.baseUrl.protocol}://${getHostName()}/${getBasePath()}/"
  }
}
