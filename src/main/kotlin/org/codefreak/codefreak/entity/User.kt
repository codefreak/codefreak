package org.codefreak.codefreak.entity

import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import org.codefreak.codefreak.auth.Role
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.userdetails.UserDetails

@Entity
class User(private val username: String) : BaseEntity(), UserDetails, CredentialsContainer {
  @Column(unique = true)
  val usernameCanonical = username.toLowerCase()

  @ElementCollection(targetClass = Role::class, fetch = FetchType.EAGER)
  @CollectionTable
  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  var roles: MutableSet<Role> = mutableSetOf()

  var firstName: String? = null

  var lastName: String? = null

  var password: String? = null
    @JvmName("_getPassword") get

  override fun getUsername() = username
  override fun getPassword() = password
  override fun getAuthorities() = roles.flatMap { it.allGrantedAuthorities }.toMutableList()
  override fun isEnabled() = true
  override fun isCredentialsNonExpired() = true
  override fun isAccountNonExpired() = true
  override fun isAccountNonLocked() = true
  override fun eraseCredentials() {
    password = null
  }

  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "userAlias", referencedColumnName = "id")
  var userAlias: UserAlias? = null
}
