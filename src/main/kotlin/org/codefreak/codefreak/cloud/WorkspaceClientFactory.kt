package org.codefreak.codefreak.cloud

import org.springframework.stereotype.Service

@Service
class WorkspaceClientFactory {
  fun createClient(workspaceReference: RemoteWorkspaceReference) = WorkspaceClient(workspaceReference)
}
