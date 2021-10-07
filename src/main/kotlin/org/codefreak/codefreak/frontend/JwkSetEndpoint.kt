package org.codefreak.codefreak.frontend

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import java.security.KeyPair
import java.security.Principal
import java.security.interfaces.RSAPublicKey
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
internal class JwkSetEndpoint(private val keyPair: KeyPair) {
  @GetMapping("/.well-known/jwks.json")
  @ResponseBody
  fun getKey(principal: Principal?): Map<String, Any> {
    val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
    val key: RSAKey = RSAKey.Builder(publicKey).build()
    return JWKSet(key).toJSONObject()
  }
}
