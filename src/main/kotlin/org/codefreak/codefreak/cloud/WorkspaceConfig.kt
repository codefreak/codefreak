package org.codefreak.codefreak.cloud

import java.io.InputStream

/**
 * Interface for describing the demand for a Workspace
 */
interface WorkspaceConfig {
  /**
   * ID that identifies the workspace externally.
   * Implementations will not create more than one workspace with this reference for each user.
   */
  val externalId: String

  /**
   * Identity of the user that will be authorized to access the workspace.
   * This should be a unique identity of the user (id or username).
   * Will be used for creating the JWT "sub" claim.
   */
  val user: String

  /**
   * Tar archive input stream that will be extracted initially to the workspace
   */
  val files: InputStream

  /**
   * Map of executable name to content of scripts that will be added to PATH
   */
  val scripts: Map<String, String>

  /**
   * Name of the container image that will be used for creating the workspace.
   * The default value will be the current companion image but teachers might be able to create
   * their own custom images in the future.
   */
  val imageName: String
}

/**
 * Reference to a workspace that has been created by an implementation.
 */
interface WorkspaceReference {
  val id: String
  val baseUrl: String
  val authToken: String
}

data class DefaultWorkspaceReference(override val id: String, override val baseUrl: String, override val authToken: String) : WorkspaceReference

interface WorkspaceService<T : WorkspaceConfig> {
  fun findWorkspace(config: T): WorkspaceReference?
  fun createWorkspace(config: T): WorkspaceReference
  fun deleteWorkspace(config: T)
}
