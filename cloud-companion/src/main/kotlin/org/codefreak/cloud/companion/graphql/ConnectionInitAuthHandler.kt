package org.codefreak.cloud.companion.graphql

import org.codefreak.codefreak.graphql.GraphQlConnectionInitException
import org.codefreak.codefreak.graphql.GraphQlConnectionInitHandler
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Handler for GQL ConnectionInit messages that performs a JWT verification
 * on the "jwt" payload field. If the authentication is successful the handler
 * will return a mono containing a map of claims.
 * If the Websocket session has already contains a principal from prior authentication
 * over HTTP headers no validation of the init message payload is performed.
 */
class ConnectionInitAuthHandler(
  private val jwtDecoder: ReactiveJwtDecoder
) : GraphQlConnectionInitHandler {

  override fun handleInit(payload: Map<String, Any>?, webSocketSession: WebSocketSession): Mono<Map<String, Any>> {
    return getClaimsFromSession(webSocketSession)
      .switchIfEmpty { performPayloadAuth(payload) }
  }

  private fun getClaimsFromSession(session: WebSocketSession): Mono<Map<String, Any>> {
    return session.handshakeInfo.principal.mapNotNull {
      if (it is JwtAuthenticationToken) {
        it.token.claims
      } else {
        null
      }
    }
  }

  private fun performPayloadAuth(payload: Map<String, Any>?): Mono<Map<String, Any>> {
    if (payload == null || payload["jwt"] == null) {
      throw GraphQlConnectionInitException.fromCode(4401, "No jwt token provided")
    }
    val jwt = payload["jwt"]
    if (jwt !is String) {
      throw GraphQlConnectionInitException.fromCode(4401, "Provided jwt is not a string")
    }
    return jwtDecoder.decode(jwt)
      .map { it.claims }
      .onErrorMap(JwtException::class.java) { e ->
        GraphQlConnectionInitException.fromCode(4401, "JWT Auth failed: ${e.message}")
      }
  }
}
