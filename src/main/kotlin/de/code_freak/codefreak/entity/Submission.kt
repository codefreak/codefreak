package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class Submission(
  id: Long? = null,

  /**
   * The user that handed in this submissions
   */
  @ManyToOne
  var user: User? = null,

  /**
   * The assignment this submission belongs to
   */
  @ManyToOne
  var assignment: Assignment? = null
) : JpaPersistable<Long>(id)
