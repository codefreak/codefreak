package org.codefreak.codefreak.service.workspace

import org.codefreak.codefreak.config.AppConfiguration

/**
 * Interface for describing the demand for a Workspace
 */
data class WorkspaceConfiguration(
  /**
   * Name of the container image that will be used for creating the workspace.
   * The default value will be the current companion image but teachers might be able to create
   * their own custom images in the future.
   */
  val imageName: String,

  /**
   * Map of executable name to content of scripts that will be added to PATH
   */
  val scripts: Map<String, String> = emptyMap(),

  /**
   * Optional map of additional environment variables that will be available
   * in the workspace.
   */
  val environment: Map<String, String>? = null,

  /**
   * CPU limit of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.cpu`.
   *
   * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
   */
  val cpuLimit: String? = null,

  /**
   * Memory limit of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.memory`.
   *
   * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
   */
  val memoryLimit: String? = null,

  /**
   * Limit for ephemeral storage of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.ephemeral-storage`.
   *
   * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
   */
  val diskLimit: String? = null
)

/**
 * Convenient method to create a new WorkspaceConfiguration based on default values defined in
 * the AppConfiguration. If imageName is null or blank the default image from the AppConfiguration will be used
 */
fun AppConfiguration.Workspaces.createWorkspaceConfiguration(
  imageName: String?,
  scripts: Map<String, String> = emptyMap(),
  environment: Map<String, String>? = null
) = WorkspaceConfiguration(
  // use the default image from AppConfiguration.Workspaces if the given image name is blank or null
  imageName?.takeIf { it.isNotBlank() } ?: companionImage,
  scripts, environment,
  // the following values are properties from AppConfiguration.Workspaces
  cpuLimit, memoryLimit, diskLimit
)
