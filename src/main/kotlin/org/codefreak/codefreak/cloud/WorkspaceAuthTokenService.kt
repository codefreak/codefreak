package org.codefreak.codefreak.cloud

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

const val SYSTEM_USER_JWT_SUB = "__system__"

@Service
class WorkspaceAuthTokenService(
  keyPair: KeyPair,
  @Value("#{@config.instanceId}") private val instanceId: String
) {
  private val rsaKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
    .privateKey(keyPair.private)
    .build()

  private val signer = RSASSASigner(rsaKey)

  fun createAuthToken(identifier: WorkspaceIdentifier): String {
    val signedJWT = SignedJWT(
      JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(rsaKey.keyID)
        .build(),
      buildWorkspaceClaimSet(identifier)
    )
    signedJWT.sign(signer)
    return signedJWT.serialize()
  }

  private fun buildWorkspaceClaimSet(identifier: WorkspaceIdentifier): JWTClaimsSet {
    return JWTClaimsSet.Builder()
      .subject(SYSTEM_USER_JWT_SUB) // TODO: Replace with actual user?
      .issuer(instanceId) // TODO: Valid uri/url?
      .audience(identifier.hashString()) // TODO: Valid uri/url?
      .expirationTime(Date.from(Instant.now().plus(12, ChronoUnit.HOURS)))
      .build()
  }
}
