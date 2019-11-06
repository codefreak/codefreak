package de.code_freak.codefreak.auth

import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.UserService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class DevUserDetailsService(private val userService: UserService) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    return try {
      userService.getUser(username)
    } catch (e: EntityNotFoundException) {
      throw UsernameNotFoundException("User $username cannot be found")
    }
  }
}
