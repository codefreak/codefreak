package org.codefreak.codefreak.auth.lti

import com.nimbusds.jwt.JWTClaimsSet
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails

class LtiAuthenticationToken(
  val user: UserDetails,
  val accessToken: String,
  val claims: JWTClaimsSet
) : AbstractAuthenticationToken(user.authorities) {
  init {
    super.setAuthenticated(true)
  }

  override fun getPrincipal() = user
  override fun getCredentials() = accessToken
}
