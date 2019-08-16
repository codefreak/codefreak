package de.code_freak.codefreak.auth.lti

import de.code_freak.codefreak.auth.AppUser
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class LtiAuthenticationToken(val appUser: AppUser, val accessToken: String, authorities: List<GrantedAuthority>) :
    AbstractAuthenticationToken(authorities) {
  init {
    super.setAuthenticated(true)
  }

  override fun getPrincipal() = appUser
  override fun getCredentials() = accessToken
}
