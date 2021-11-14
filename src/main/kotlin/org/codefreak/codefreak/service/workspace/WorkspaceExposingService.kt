package org.codefreak.codefreak.service.workspace

import java.net.URI

/**
 * A Workspace exposing service is responsible for making a workspace API available
 * via a public URL. Most commonly this is done via reverse proxies but other mechanism
 * might be suitable in some scenarios like exposing via random server ports.
 */
interface WorkspaceExposingService {
  /**
   * Create a unique url for a workspace. The function must return the same URL for identical identifiers.
   * This method should return a URL even in cases where the workspace does not exist (yet/anymore).
   */
  fun createWorkspaceUrl(workspaceIdentifier: WorkspaceIdentifier): URI

  /**
   * Create the necessary infrastructure resources for exposing the given service.
   * The method is called before the workspace is created so an implementation has the option
   * to make necessary tweaks to the WorkspaceConfiguration like adding labels.
   */
  fun exposeWorkspace(workspaceIdentifier: WorkspaceIdentifier)

  /**
   * This method is called after a workspace has been torn down to perform necessary clean up operations.
   */
  fun unexposeWorkspace(workspaceIdentifier: WorkspaceIdentifier)
}
