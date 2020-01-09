package de.code_freak.codefreak.auth

import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.authority.SimpleGrantedAuthority

class Authorization(val currentUser: User = FrontendUtil.getCurrentUser()) {

  companion object {
    fun deny(): Nothing = throw AccessDeniedException("AccessDenied")
  }

  fun requireAuthority(authority: String) {
    if (!currentUser.authorities.contains(SimpleGrantedAuthority(authority))) {
      deny()
    }
  }

  fun isCurrentUser(user: User) = user == currentUser

  fun requireAuthorityIfNotCurrentUser(user: User, authority: String) {
    if (!isCurrentUser(user)) {
      requireAuthority(authority)
    }
  }

  fun requireIsCurrentUser(user: User) {
    if (!isCurrentUser(user)) {
      deny()
    }
  }
}
