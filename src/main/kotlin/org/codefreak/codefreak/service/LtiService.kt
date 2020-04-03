package org.codefreak.codefreak.service

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.CachedJwtClaimsSet
import org.codefreak.codefreak.repository.CachedJwtClaimsSetRepository
import net.minidev.json.JSONArray
import org.mitre.jwt.signer.service.JWTSigningAndValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
@ConditionalOnProperty("codefreak.lti.enabled")
class LtiService {
  companion object {
    const val CLAIM_DEPLOYMENT_ID = "https://purl.imsglobal.org/spec/lti/claim/deployment_id"
    const val CLAIM_MESSAGE_TYPE = "https://purl.imsglobal.org/spec/lti/claim/message_type"
    const val CLAIM_VERSION = "https://purl.imsglobal.org/spec/lti/claim/version"
    const val CLAIM_CONTENT_ITEMS = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items"
  }

  @Autowired
  lateinit var signingAndValidationService: JWTSigningAndValidationService

  @Autowired
  lateinit var cachedJwtClaimsSetRepository: CachedJwtClaimsSetRepository

  fun findCachedJwtClaimsSet(id: UUID): JWTClaimsSet = cachedJwtClaimsSetRepository.findById(id)
      .map { JWTClaimsSet.parse(it.serializedClaimSet) }
      .orElseThrow { EntityNotFoundException("JWT could not be found") }

  fun cacheJwtClaimsSet(claimsSet: JWTClaimsSet): UUID = cachedJwtClaimsSetRepository.save(
      CachedJwtClaimsSet(claimsSet.toString())
  ).id

  fun removeCachedJwtClaimSet(id: UUID) = cachedJwtClaimsSetRepository.deleteById(id)

  /**
   * Create a LTIDeepLinkingResponse JWT that links to an assignment
   */
  fun buildDeepLinkingResponse(requestJwt: JWTClaimsSet, assignment: Assignment, launchUrl: String): SignedJWT {
    val jwsAlgorithm = signingAndValidationService.defaultSigningAlgorithm
    val contentItems = JSONArray()
    contentItems.appendElement(
        mapOf(
            "type" to "link",
            "title" to "${assignment.title} (Code FREAK)",
            "url" to launchUrl,
            "window" to mapOf(
                "targetName" to "codefreak-${assignment.id}"
            )
        )
    )
    val jwt = SignedJWT(
        JWSHeader(jwsAlgorithm),
        genericResponseClaimSetBuilder(requestJwt)
            .claim(CLAIM_MESSAGE_TYPE, "LTIDeepLinkingResponse")
            .claim(CLAIM_CONTENT_ITEMS, contentItems)
            .build()
    )
    signingAndValidationService.signJwt(jwt)
    return jwt
  }

  /**
   * Create a basic JWT claim set that responds to an incoming claim set by swapping issuer and audience
   * and carrying over some basic claims
   */
  fun genericResponseClaimSetBuilder(requestJwt: JWTClaimsSet): JWTClaimsSet.Builder {
    val exp = Date(System.currentTimeMillis() + 60 * 1000)
    val now = Date()
    return JWTClaimsSet.Builder()
        .issuer(requestJwt.audience[0])
        .audience(requestJwt.issuer)
        .expirationTime(exp)
        .issueTime(now)
        .notBeforeTime(now)
        .claim(CLAIM_VERSION, "1.3.0")
        .claim(CLAIM_DEPLOYMENT_ID, requestJwt.getStringClaim(CLAIM_DEPLOYMENT_ID))
  }
}
