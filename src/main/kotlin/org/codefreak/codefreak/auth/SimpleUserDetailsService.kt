package org.codefreak.codefreak.auth

import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.UserService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class SimpleUserDetailsService(private val userService: UserService) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    return try {
      userService.getUser(username)
    } catch (e: EntityNotFoundException) {
      throw UsernameNotFoundException("User $username cannot be found")
    }
  }
}
