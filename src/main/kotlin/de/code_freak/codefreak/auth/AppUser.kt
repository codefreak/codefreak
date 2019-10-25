package de.code_freak.codefreak.auth

import de.code_freak.codefreak.entity.User
import org.springframework.security.core.userdetails.User as SpringUser

class AppUser(
  val entity: User,
  password: String = ""
) : SpringUser(
    entity.username,
    password,
    entity.roles.flatMap { it.allGrantedAuthorities }
) {
  val displayName = listOfNotNull(entity.firstName, entity.lastName).ifEmpty { listOf(username) }.joinToString(" ")
}
