package de.code_freak.codefreak.auth.lti

import com.nimbusds.jwt.JWTClaimsSet
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class LtiAuthenticationToken(
  val user: UserDetails,
  val accessToken: String,
  authorities: List<GrantedAuthority>,
  val claims: JWTClaimsSet
) : AbstractAuthenticationToken(authorities) {
  init {
    super.setAuthenticated(true)
  }

  override fun getPrincipal() = user.username
  override fun getCredentials() = accessToken
}
