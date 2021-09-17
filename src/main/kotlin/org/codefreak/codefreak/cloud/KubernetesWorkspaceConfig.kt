package org.codefreak.codefreak.cloud

import java.util.UUID
import org.codefreak.codefreak.config.AppConfiguration
import java.io.InputStream

data class KubernetesWorkspaceConfig(
  private val appConfig: AppConfiguration,
  /**
   * A unique external ID the workspace refers to
   */
  private val reference: UUID,
  private val filesSupplier: () -> InputStream
): WorkspaceConfig {
  override val externalId = reference.toString()
  override val user = ""
  override val files
    get() = filesSupplier()
  override val scripts: MutableMap<String, String> = mutableMapOf()
  override val imageName = appConfig.workspaces.companionImage

  val workspaceId = reference.toString().lowercase()
  val persistentVolumeClaimName = workspaceId.lowercase() + "-data"
  val persistentVolumeName = workspaceId.lowercase() + "-data"
  val companionDeploymentName = workspaceId.lowercase() + "-companion"
  val companionIngressName = workspaceId.lowercase() + "-companion"
  val companionScriptMapName = workspaceId.lowercase() + "-scripts"

  val baseUrl
    get() = appConfig.workspaces.baseUrl

  // Service names are limited to 63 characters...
  val companionServiceName = "ws-${reference.toString().split("-").first()}"

  fun getLabels(): Map<String, String> {
    return mapOf(
        "org.codefreak.instance-id" to appConfig.instanceId,
        "org.codefreak.workspace-id" to workspaceId
    )
  }

  fun getLabelsForComponent(componentName: String): Map<String, String> {
    return getLabels() + mapOf(
        "app.kubernetes.io/component" to componentName
    )
  }

  fun addScript(name: String, content: String) {
    scripts[name] = content
  }
}
