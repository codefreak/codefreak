package org.codefreak.cloud.companion.security

import org.springframework.graphql.web.WebSocketInterceptor
import reactor.core.publisher.Mono

/**
 * Interceptor for GQL ConnectionInit messages that performs a JWT verification
 * on the "jwt" payload field. If the authentication is successful the handler
 * will return a mono containing a map of claims.
 */
class ConnectionInitAuthInterceptor(
  private val jwtWebsocketAuthenticationService: JwtWebsocketAuthenticationService
) : WebSocketInterceptor {

  override fun handleConnectionInitialization(payload: MutableMap<String, Any>): Mono<Any> {
    return jwtWebsocketAuthenticationService.authenticateByPayload(payload) as Mono<Any>
  }
}
