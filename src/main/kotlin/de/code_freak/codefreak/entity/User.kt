package de.code_freak.codefreak.entity

import de.code_freak.codefreak.auth.Role
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType

@Entity
class User(
  val username: String,
  @ElementCollection(targetClass = Role::class, fetch = FetchType.EAGER)
  @CollectionTable
  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  var roles: Set<Role> = setOf(),
  var firstName: String? = null,
  var lastName: String? = null
) : BaseEntity()
