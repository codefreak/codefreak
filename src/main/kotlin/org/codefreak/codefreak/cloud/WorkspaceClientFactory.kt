package org.codefreak.codefreak.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class WorkspaceClientFactory(
  private val objectMapper: ObjectMapper
) {
  fun createClient(remoteWorkspaceReference: RemoteWorkspaceReference) = WorkspaceClient(remoteWorkspaceReference, objectMapper)
}
