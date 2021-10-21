package org.codefreak.codefreak.frontend

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import java.security.Principal
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * This endpoint makes the JWK set available for workspaces.
 * Its publicly accessible as it does not expose any sensitive information but only the public
 * key to verify signatures of generated tokens.
 */
@ConditionalOnBean(RSAKey::class)
@RestController
internal class JwkSetEndpoint(rsaKey: RSAKey) {
  /**
   * This will create a JWK with the public keys only
   */
  private val jwkSet = JWKSet(rsaKey).toJSONObject(true)

  @GetMapping("/.well-known/jwks.json", produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  fun getKey(principal: Principal?): Map<String, Any> {
    return jwkSet
  }
}
