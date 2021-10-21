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

  /**
   * Authenticate only via the given key-value pairs. The payload must contain a "jwt" key.
   */
  fun authenticateByPayload(payload: Map<String, Any>?): Mono<Map<String, Any>> {
    return performPayloadAuth(payload)
      .switchIfEmpty(Mono.error(JwtAuthenticationException("Payload does not contain a jwt auth token")))
  }

  /**
   * Authenticate either via principal from websocket session or via "jwt" from payload.
   * The payload must contain a "jwt" key.
   */
  fun authenticateWebsocketSession(session: WebSocketSession, payload: Map<String, Any>?): Mono<Map<String, Any>> {
    return getClaimsFromSession(session)
      .switchIfEmpty(performPayloadAuth(payload))
      .switchIfEmpty(Mono.error(JwtAuthenticationException("Neither HTTP request nor payload contains a jwt auth token")))
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
    if (payload == null) {
      return Mono.empty()
    }
    return Mono.justOrEmpty(payload).flatMap {
      if (it["jwt"] == null) {
        return@flatMap Mono.error(JwtAuthenticationException("No jwt token provided"))
      }
      val jwt = it["jwt"]
      if (jwt !is String) {
        return@flatMap Mono.error(JwtAuthenticationException("Provided jwt is not a string"))
      }
      jwtDecoder.decode(jwt)
        .map { decodedJwt -> decodedJwt.claims }
        .onErrorMap(JwtException::class.java) { e -> JwtAuthenticationException("JWT Auth failed", e) }
    }
  }
}
