package org.codefreak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
class UserAlias(

  @OneToOne(mappedBy ="userAlias")
  var user: User,

  /**
   * Some Rng Name
   */
  var alias : String

) : BaseEntity()
