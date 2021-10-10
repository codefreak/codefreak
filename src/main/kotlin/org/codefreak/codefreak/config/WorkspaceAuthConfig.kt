package org.codefreak.codefreak.config

import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
  name = [
    "codefreak.workspaces.jwt-private-key-file",
    "codefreak.workspaces.jwt-public-key-file"
  ]
)
class WorkspaceAuthConfig {
  @Bean
  fun rsaKey(
    @Value("\${codefreak.workspaces.jwt-private-key-file}") privateKeyPath: String,
    @Value("\${codefreak.workspaces.jwt-public-key-file}") publicKeyPath: String
  ): RSAKey {
    val publicKey = KeyFactory.getInstance("RSA")
      .generatePublic(X509EncodedKeySpec(loadKeyAsBytes(publicKeyPath))) as RSAPublicKey
    val privateKey = KeyFactory.getInstance("RSA")
      .generatePrivate(X509EncodedKeySpec(loadKeyAsBytes(privateKeyPath))) as RSAPrivateKey
    return RSAKey.Builder(publicKey)
      .privateKey(privateKey)
      .build()
  }

  private fun loadKeyAsBytes(keyPath: String): ByteArray {
    val content = File(keyPath).readText()
      .replace("/-----(:?BEGIN|END) (:?PUBLIC|PRIVATE) KEY-----/g".toRegex(), "")
      .trim()
    return Base64.getMimeDecoder().decode(content)
  }

  @Bean
  fun jwsSigner(rsaKey: RSAKey): JWSSigner {
    return RSASSASigner(rsaKey)
  }
}
