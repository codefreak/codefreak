package org.codefreak.codefreak.service.workspace

/**
 * Interface for describing the demand for a Workspace
 */
data class WorkspaceConfiguration(
  /**
   * Map of executable name to content of scripts that will be added to PATH
   */
  val scripts: Map<String, String>,

  /**
   * Name of the container image that will be used for creating the workspace.
   * The default value will be the current companion image but teachers might be able to create
   * their own custom images in the future.
   */
  val imageName: String,

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
  val cpuLimit: String = "1",

  /**
   * Memory limit of the workspace. Must be a valid value for Kubernetes `spec.containers[].resources.limits.memory`.
   *
   * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers">Kubernetes | Managing Resources for Containers</a>
   */
  val memoryLimit: String = "1Gi"
)
