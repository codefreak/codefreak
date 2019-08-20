package de.code_freak.codefreak.auth.lti

import com.nimbusds.jwt.JWTClaimsSet
import de.code_freak.codefreak.auth.AppUser
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class LtiAuthenticationToken(
  val appUser: AppUser,
  val accessToken: String,
  authorities: List<GrantedAuthority>,
  val claims: JWTClaimsSet
) : AbstractAuthenticationToken(authorities) {
  init {
    super.setAuthenticated(true)
  }

  override fun getPrincipal() = appUser
  override fun getCredentials() = accessToken
}
