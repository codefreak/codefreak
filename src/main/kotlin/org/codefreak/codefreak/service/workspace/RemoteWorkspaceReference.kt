package org.codefreak.codefreak.service.workspace

/**
 * Reference to a workspace that has been created by an implementation.
 * It contains all information that is required to connect to a workspace.
 */
data class RemoteWorkspaceReference(
  val identifier: WorkspaceIdentifier,
  val baseUrl: String
)
