package de.code_freak.codefreak.entity

import javax.persistence.Entity

@Entity
class User(
  id: Long? = null
) : JpaPersistable<Long>(id)
