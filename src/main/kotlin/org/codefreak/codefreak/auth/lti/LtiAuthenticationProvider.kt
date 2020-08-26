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

  // Role mapping is currently disabled because they are course-based and we only support global roles
  private val roleMap = mapOf<String, Role>()

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
    // merge roles from LTI and roles from DB
    user.roles.addAll(roles)
    return user
  }

  private fun buildRoles(claims: JWTClaimsSet): List<Role> {
    val roles = claims.getStringArrayClaim("https://purl.imsglobal.org/spec/lti/claim/roles")
    return roles.mapNotNull { role -> roleMap[role] }.ifEmpty { listOf(Role.STUDENT) }
  }

  override fun supports(authentication: Class<*>?): Boolean {
    return authentication != null && PendingLtiAuthenticationToken::class.java.isAssignableFrom(authentication)
  }
}
