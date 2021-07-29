package org.codefreak.codefreak.cloud

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import org.springframework.stereotype.Service
import java.net.URI

@Service
class WorkspaceClientFactory {
  fun createClient(reference: RemoteWorkspaceReference): ApolloClient {
    return ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = buildGraphqlUrl(reference.baseUrl),
            protocol = GraphQLWsProtocol()
        )
    )
  }

  private fun buildGraphqlUrl(workspaceBaseUrl: String): String {
    val workspaceUri = URI.create(workspaceBaseUrl)
    return URI(
        if (workspaceUri.scheme == "https") "wss" else "ws",
        workspaceUri.authority,
        workspaceUri.path.trimEnd('/') + "/graphql",
        "", // query
        "" // fragment
    ).toString()
  }
}
