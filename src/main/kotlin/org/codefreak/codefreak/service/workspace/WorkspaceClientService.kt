package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class WorkspaceClientService(
  private val objectMapper: ObjectMapper
) {
  fun createClient(remoteWorkspaceReference: RemoteWorkspaceReference): WorkspaceClient {
    return WorkspaceClient(remoteWorkspaceReference.baseUrl, objectMapper)
  }
}
