package de.code_freak.codefreak.auth

import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper
import java.lang.UnsupportedOperationException

class LdapUserDetailsContextMapper(
  private val userRepository: UserRepository,
  roleMappings: Map<String, Role>
) : UserDetailsContextMapper {

  private val log = LoggerFactory.getLogger(this::class.java)
  private val mappings = roleMappings.mapKeys { "ROLE_" + it.key.toUpperCase() }

  override fun mapUserToContext(user: UserDetails?, ctx: DirContextAdapter?) {
    throw UnsupportedOperationException()
  }

  override fun mapUserFromContext(ctx: DirContextOperations?, username: String?, authorities: MutableCollection<out GrantedAuthority>?): UserDetails {
    val roles = mutableListOf<Role>()

    authorities?.forEach {
      val role = mappings[it.authority]
      if (role != null) {
        roles.add(role)
      }
    }

    val user = userRepository.findByUsernameIgnoreCase(username!!).orElseGet { userRepository.save(User(username)) }
    log.debug("Logging in ${user.username} with roles $roles")
    return AppUser(user, roles)
  }
}
