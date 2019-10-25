package de.code_freak.codefreak.auth

import de.code_freak.codefreak.service.SeedDatabaseService
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.provisioning.InMemoryUserDetailsManager

class DevUserDetailsService(
  private val users: List<AppUser> = listOf(
      AppUser(SeedDatabaseService.admin, "{noop}123"),
      AppUser(SeedDatabaseService.teacher, "{noop}123"),
      AppUser(SeedDatabaseService.student, "{noop}123")
  )
) : InMemoryUserDetailsManager(users) {
  private val log = LoggerFactory.getLogger(this::class.java)

  init {
      log.warn("Sample user authentication is activated. You should not see this in production!")
  }

  // We override this to return the actual AppUser which the super class somehow doesn't.
  override fun loadUserByUsername(username: String?): UserDetails {
    return users.find { it.username == username } ?: throw UsernameNotFoundException(username)
  }
}
