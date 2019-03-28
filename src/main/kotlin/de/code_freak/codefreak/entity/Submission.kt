package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class Submission(
  /**
   * The user that handed in this submissions
   */
  @ManyToOne
  var user: User,

  /**
   * The assignment this submission belongs to
   */
  @ManyToOne
  var assignment: Assignment
) : JpaPersistable()
