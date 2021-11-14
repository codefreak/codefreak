package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.ConcurrentReferenceHashMap

@Service
class WorkspaceClientService(
  private val objectMapper: ObjectMapper,
  @Autowired(required = false)
  private val authService: WorkspaceAuthService?,
  @Value("#{@config.workspaces.ingress.disableTlsVerification}")
  private val disableTlsVerification: Boolean = false
) {
  private val existingClients = ConcurrentReferenceHashMap<RemoteWorkspaceReference, WorkspaceClient>()

  /**
   * Get a workspace client for the given workspace.
   * This will recycle existing clients.
   */
  fun getClient(remoteWorkspaceReference: RemoteWorkspaceReference): WorkspaceClient {
    return existingClients.computeIfAbsent(remoteWorkspaceReference) { createClient(remoteWorkspaceReference) }
  }

  /**
   * Create a fresh workspace client for the given workspace
   */
  private fun createClient(remoteWorkspaceReference: RemoteWorkspaceReference): WorkspaceClient {
    return WorkspaceClient(
      baseUrl = remoteWorkspaceReference.baseUrl,
      authToken = createAuthToken(remoteWorkspaceReference),
      objectMapper,
      disableTlsVerification
    )
  }

  fun requiresAuthentication(): Boolean {
    return authService != null
  }

  private fun createAuthToken(remoteWorkspaceReference: RemoteWorkspaceReference): String? {
    return authService?.createSystemAuthToken(remoteWorkspaceReference.identifier)
  }
}
