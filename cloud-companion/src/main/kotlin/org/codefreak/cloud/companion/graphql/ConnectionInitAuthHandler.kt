package org.codefreak.cloud.companion.graphql

import org.codefreak.codefreak.graphql.GraphQlConnectionInitException
import org.codefreak.codefreak.graphql.GraphQlConnectionInitHandler
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono

class ConnectionInitAuthHandler(
  private val jwtDecoder: ReactiveJwtDecoder
) : GraphQlConnectionInitHandler {

  override fun handleInit(payload: MutableMap<String, Any>?): Mono<MutableMap<String, Any>> {
    if (payload == null || payload["jwt"] == null) {
      throw GraphQlConnectionInitException.fromCode(4401, "No jwt token provided")
    }
    val jwt = payload["jwt"]
    if (jwt !is String) {
      throw GraphQlConnectionInitException.fromCode(4401, "Provided jwt is not a string")
    }
    try {
      return jwtDecoder.decode(jwt).map {
        it.claims
      }
    } catch (e: JwtException) {
      throw GraphQlConnectionInitException.fromCode(4401, "JWT Auth failed: ${e.message}")
    }
  }
}
