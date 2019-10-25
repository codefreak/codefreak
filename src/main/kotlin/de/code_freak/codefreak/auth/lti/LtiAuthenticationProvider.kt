package de.code_freak.codefreak.auth.lti

import com.nimbusds.jwt.JWTClaimsSet
import de.code_freak.codefreak.auth.Role
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.service.UserService
import org.mitre.openid.connect.client.OIDCAuthenticationProvider
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication

class LtiAuthenticationProvider(private val userService: UserService) : OIDCAuthenticationProvider() {

  private val log = LoggerFactory.getLogger(this::class.java)

  private val authorityMap = mapOf(
      "http://purl.imsglobal.org/vocab/lis/v2/system/person#Administrator" to Role.ADMIN
      // Memberships depend on the context (course) and cannot be assigned as global role
      //"http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor" to Role.TEACHER
  )

  /**
   * Authenticate the user based on the data we received via JWT
   */
  override fun authenticate(authentication: Authentication?): Authentication? {
    if (authentication !is PendingOIDCAuthenticationToken) {
      return null
    }
    val idToken = authentication.idToken
    val claims = idToken.jwtClaimsSet

    val roles = buildAuthorities(claims)
    return LtiAuthenticationToken(
        buildUser(claims, roles),
        authentication.accessTokenValue,
        roles,
        claims
    )
  }

  private fun buildUser(claims: JWTClaimsSet, roles: List<Role>): User {
    val username = claims.getStringClaim("email")
    val user = userService.getOrCreateUser(username) {
      this.roles = roles.toSet()
      firstName = claims.getStringClaim("given_name")
      lastName = claims.getStringClaim("family_name")
    }
    log.debug("Logging in ${user.username} with roles $roles")
    return user
  }

  private fun buildAuthorities(claims: JWTClaimsSet): List<Role> {
    val roles = claims.getStringArrayClaim("https://purl.imsglobal.org/spec/lti/claim/roles")
    return roles.mapNotNull { role -> authorityMap[role] }
  }

  override fun supports(authentication: Class<*>?): Boolean {
    return authentication != null && PendingLtiAuthenticationToken::class.java.isAssignableFrom(authentication)
  }
}
