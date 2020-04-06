package org.codefreak.codefreak.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

enum class Role(private val auth: String, private vararg val inheritedRoles: Role) : GrantedAuthority {
  STUDENT(Authority.ROLE_STUDENT),
  TEACHER(Authority.ROLE_TEACHER, STUDENT),
  ADMIN(Authority.ROLE_ADMIN, TEACHER, STUDENT);

  override fun getAuthority(): String = auth

  val allAuthorities get() = listOf(this, *inheritedRoles).map { it.auth }
  val allGrantedAuthorities get() = allAuthorities.map { SimpleGrantedAuthority(it) }
}
