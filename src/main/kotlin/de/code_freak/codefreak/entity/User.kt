package de.code_freak.codefreak.entity

import javax.persistence.Entity

@Entity
class User(val username: String) : BaseEntity()
