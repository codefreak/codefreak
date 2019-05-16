package de.code_freak.codefreak.entity

import javax.persistence.Entity

@Entity
class DemoUser(
  var email: String,
  var group: String
) : BaseEntity()
