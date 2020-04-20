package org.codefreak.codefreak.auth.lti

import com.nimbusds.jwt.JWTClaimsSet
import org.codefreak.codefreak.auth.Role
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.service.UserService
import org.mitre.openid.connect.client.OIDCAuthenticationProvider
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication

class LtiAuthenticationProvider(private val userService: UserService) : OIDCAuthenticationProvider() {

  private val log = LoggerFactory.getLogger(this::class.java)

  private val roleMap = mapOf(
      "http://purl.imsglobal.org/vocab/lis/v2/system/person#Administrator" to Role.ADMIN
      // Memberships depend on the context (course) and cannot be assigned as global role
      //"http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor" to Role.TEACHER
  )

  /**
   * Authenticate the user based on the data we received via JWT
   */
  override fun authenticate(authentication: Authentication?): Authentication? {
    if (authentication !is PendingLtiAuthenticationToken) {
      return null
    }
    val idToken = authentication.idToken
    val claims = idToken.jwtClaimsSet

    val roles = buildRoles(claims)
    val user = buildUser(claims, roles)
    log.debug("Logging in ${user.username} with roles $roles")
    return LtiAuthenticationToken(
        user,
        authentication.accessTokenValue,
        claims
    )
  }

  private fun buildUser(claims: JWTClaimsSet, roles: List<Role>): User {
    val username = claims.getStringClaim("email")
    val user = userService.getOrCreateUser(username) {
      firstName = claims.getStringClaim("given_name")
      lastName = claims.getStringClaim("family_name")
    }
    // roles from LTI should not be persisted
    user.roles = roles.toMutableSet()
    return user
  }

  private fun buildRoles(claims: JWTClaimsSet): List<Role> {
    val roles = claims.getStringArrayClaim("https://purl.imsglobal.org/spec/lti/claim/roles")
    return roles.mapNotNull { role -> roleMap[role] }
  }

  override fun supports(authentication: Class<*>?): Boolean {
    return authentication != null && PendingLtiAuthenticationToken::class.java.isAssignableFrom(authentication)
  }
}
