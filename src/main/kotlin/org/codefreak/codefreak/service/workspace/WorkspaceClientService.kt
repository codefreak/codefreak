package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceClientService(
  private val objectMapper: ObjectMapper,
  @Autowired(required = false)
  private val authService: WorkspaceAuthService?
) {
  fun createClient(remoteWorkspaceReference: RemoteWorkspaceReference): WorkspaceClient {
    return WorkspaceClient(
      baseUrl = remoteWorkspaceReference.baseUrl,
      authToken = createAuthToken(remoteWorkspaceReference),
      objectMapper
    )
  }

  fun requiresAuthentication(): Boolean {
    return authService != null
  }

  private fun createAuthToken(remoteWorkspaceReference: RemoteWorkspaceReference): String? {
    return authService?.createSystemAuthToken(remoteWorkspaceReference.identifier)
  }
}
