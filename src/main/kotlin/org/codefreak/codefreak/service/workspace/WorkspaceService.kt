package org.codefreak.codefreak.service.workspace

interface WorkspaceService {
  /**
   * Create a workspaces with the given identifier. If a workspace already exist it should not create a new one
   * but return a reference to the already existing one.
   * If there is no existing workspace create a new one with the given configuration.
   */
  fun createWorkspace(identifier: WorkspaceIdentifier, config: WorkspaceConfiguration): RemoteWorkspaceReference
  fun deleteWorkspace(identifier: WorkspaceIdentifier)
  fun findAllWorkspaces(): List<RemoteWorkspaceReference>

  /**
   * Trigger a file save on the given workspace. This will take the current file state from
   * the workspace and update the collection this workspace is based on in the database.
   * Implementations should not perform any updates if this workspace is marked as read-only.
   */
  fun saveWorkspaceFiles(identifier: WorkspaceIdentifier)

  /**
   * Deploy the files again from the collection the workspace is based on.
   * This should be used in cases where the collection has been changed externally.
   * The operation is destructive as it will override all files existing in the workspace.
   */
  fun redeployWorkspaceFiles(identifier: WorkspaceIdentifier)
}
