package de.code_freak.codefreak.auth

import de.code_freak.codefreak.entity.User
import org.springframework.security.core.userdetails.User as SpringUser

class AppUser(
  val entity: User,
  roles: Collection<Role>,
  password: String = "",
  firstName: String? = null,
  lastName: String? = null
) : SpringUser(
    entity.username,
    password,
    roles.flatMap { it.allGrantedAuthorities }
) {
  val displayName = listOfNotNull(firstName, lastName).ifEmpty { listOf(username) }.joinToString(" ")
}
