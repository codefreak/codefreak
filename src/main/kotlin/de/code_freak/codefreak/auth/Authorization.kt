package de.code_freak.codefreak.auth

import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.authority.SimpleGrantedAuthority

object Authorization {

  fun requireAuthority(authority: String) {
    if (!FrontendUtil.getCurrentUser().authorities.contains(SimpleGrantedAuthority(authority))) {
      throw AccessDeniedException("Access Denied")
    }
  }

  fun isCurrentUser(user: User) = user == FrontendUtil.getCurrentUser()

  fun requireIsCurrentUser(user: User) {
    if (!isCurrentUser(user)) {
      throw AccessDeniedException("Access Denied")
    }
  }
}
