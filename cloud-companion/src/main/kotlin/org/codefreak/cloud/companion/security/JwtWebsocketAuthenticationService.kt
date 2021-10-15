package org.codefreak.cloud.companion.security

import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

/**
 * The JWT WS Authentication service is responsible for authenticating websocket sessions
 * by the provided "jwt" payload. Currently, it's only used for GraphQL but will also
 * be utilized for process websockets in the future.
 */
class JwtWebsocketAuthenticationService(private val jwtDecoder: ReactiveJwtDecoder) {
  class JwtAuthenticationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

  fun authenticateWebsocketSession(session: WebSocketSession, payload: Map<String, Any>?): Mono<Map<String, Any>> {
    return getClaimsFromSession(session).switchIfEmpty(performPayloadAuth(payload))
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
      return Mono.error(JwtAuthenticationException("No jwt token provided"))
    }
    val jwt = payload["jwt"]
    if (jwt !is String) {
      return Mono.error(JwtAuthenticationException("Provided jwt is not a string"))
    }
    return jwtDecoder.decode(jwt)
      .map { it.claims }
      .onErrorMap(JwtException::class.java) { JwtAuthenticationException("JWT Auth failed", it) }
  }
}
