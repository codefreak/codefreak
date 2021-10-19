package org.codefreak.cloud.companion.security

import org.codefreak.codefreak.graphql.GraphQlConnectionInitException
import org.codefreak.codefreak.graphql.GraphQlConnectionInitHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

/**
 * Handler for GQL ConnectionInit messages that performs a JWT verification
 * on the "jwt" payload field. If the authentication is successful the handler
 * will return a mono containing a map of claims.
 * If the Websocket session has already contains a principal from prior authentication
 * over HTTP headers no validation of the init message payload is performed.
 */
class ConnectionInitAuthHandler(
  private val jwtWebsocketAuthenticationService: JwtWebsocketAuthenticationService
) : GraphQlConnectionInitHandler {

  override fun handleInit(payload: Map<String, Any>?, webSocketSession: WebSocketSession): Mono<Map<String, Any>> {
    return jwtWebsocketAuthenticationService.authenticateWebsocketSession(webSocketSession, payload)
      .onErrorMap { GraphQlConnectionInitException.fromCode(4401, it.message) }
  }
}
