package org.codefreak.codefreak.service.workspace

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import org.codefreak.codefreak.entity.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

const val SYSTEM_USER_JWT_SUB = "__system__"

@Service
@ConditionalOnBean(value = [JWSSigner::class, RSAKey::class])
class WorkspaceAuthService(
  private val rsaKey: RSAKey,
  private val jwsSigner: JWSSigner,
  @Value("#{@config.instanceId}") private val instanceId: String
) {
  companion object {
    private val log = LoggerFactory.getLogger(WorkspaceAuthService::class.java)
  }

  /**
   * Create an auth token that should only ever be used when communicating from Code FREAK to a workspace
   */
  fun createSystemAuthToken(identifier: WorkspaceIdentifier): String {
    return createSignedJwt(identifier, SYSTEM_USER_JWT_SUB).serialize()
  }

  /**
   * Create an auth token that grants the given user access to a workspace.
   */
  fun createUserAuthToken(identifier: WorkspaceIdentifier, user: User): String {
    return createSignedJwt(identifier, user.usernameCanonical).serialize()
  }

  private fun createSignedJwt(identifier: WorkspaceIdentifier, subject: String): SignedJWT {
    val jwtHeader = JWSHeader.Builder(JWSAlgorithm.RS256)
      .keyID(rsaKey.keyID)
      .build()
    val jwt = SignedJWT(jwtHeader, buildWorkspaceClaimSet(identifier, subject))
    jwt.sign(jwsSigner)
    return jwt.also {
      log.debug("Created a JWT for $identifier: ${it.serialize()}")
    }
  }

  private fun buildWorkspaceClaimSet(identifier: WorkspaceIdentifier, subject: String): JWTClaimsSet {
    return JWTClaimsSet.Builder()
      .subject(subject)
      .issuer(instanceId) // TODO: Valid uri/url?
      .audience(identifier.hashString()) // TODO: Valid uri/url?
      .expirationTime(Date.from(Instant.now().plus(12, ChronoUnit.HOURS)))
      .build()
  }
}
