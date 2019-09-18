package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

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
) : BaseEntity() {
  /**
   * List of answers in this submission
   */
  @OneToMany(mappedBy = "submission")
  var answers: MutableSet<Answer> = mutableSetOf()
}
