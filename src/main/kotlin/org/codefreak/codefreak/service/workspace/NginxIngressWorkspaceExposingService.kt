package org.codefreak.codefreak.service.workspace

import io.fabric8.kubernetes.client.KubernetesClient
import java.net.URI
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.service.workspace.model.WorkspaceNginxIngressModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

/**
 * Exposes workspaces by create a Nginx-specific Ingress resource in the configured
 * Kubernetes cluster.
 */
@Service
class NginxIngressWorkspaceExposingService(
  kubernetesClient: KubernetesClient,
  @Value("#{@config.workspaces.ingress}")
  private val ingressConfig: AppConfiguration.Workspaces.Ingress
) : WorkspaceExposingService {
  private val ingressApi = kubernetesClient.network().v1().ingresses()

  override fun createWorkspaceUrl(workspaceIdentifier: WorkspaceIdentifier): URI {
    val urlVariables = mapOf(
      "workspaceIdentifier" to workspaceIdentifier.hashString()
    )
    return UriComponentsBuilder.fromUriString(ingressConfig.baseUrlTemplate)
      .buildAndExpand(urlVariables)
      .toUri()
  }

  override fun exposeWorkspace(workspaceIdentifier: WorkspaceIdentifier) {
    ingressApi.createOrReplaceWithLog(
      WorkspaceNginxIngressModel(workspaceIdentifier, createWorkspaceUrl(workspaceIdentifier), ingressConfig)
    )
  }

  override fun unexposeWorkspace(workspaceIdentifier: WorkspaceIdentifier) {
    ingressApi.withName(workspaceIdentifier.workspaceIngressName).delete()
  }
}
