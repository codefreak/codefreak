package org.codefreak.codefreak.auth

import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.util.FrontendUtil
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.authority.SimpleGrantedAuthority

class Authorization(val currentUser: User = FrontendUtil.getCurrentUser()) {

  companion object {
    fun deny(): Nothing = throw AccessDeniedException("AccessDenied")
  }

  fun requireAuthority(authority: String) {
    if (!currentUser.hasAuthority(authority)) {
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

fun User.hasAuthority(authority: String) = authorities.contains(SimpleGrantedAuthority(authority))
