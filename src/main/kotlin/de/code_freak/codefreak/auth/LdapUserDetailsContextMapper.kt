package de.code_freak.codefreak.auth

import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.UserRepository
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper
import java.lang.UnsupportedOperationException

class LdapUserDetailsContextMapper(private val userRepository: UserRepository) : UserDetailsContextMapper {
  override fun mapUserToContext(user: UserDetails?, ctx: DirContextAdapter?) {
    throw UnsupportedOperationException()
  }

  val mapping = mapOf("ROLE_SHIP_CREW" to Role.ADMIN)

  override fun mapUserFromContext(ctx: DirContextOperations?, username: String?, authorities: MutableCollection<out GrantedAuthority>?): UserDetails {
    val roles = mutableListOf<Role>()

    authorities?.forEach {
      val role = mapping[it.authority]
      if (role != null) {
        roles.add(role)
      }
    }

    val user = userRepository.findByUsernameIgnoreCase(username!!).orElseGet { userRepository.save(User(username)) }
    return AppUser(user, roles)
  }
}
