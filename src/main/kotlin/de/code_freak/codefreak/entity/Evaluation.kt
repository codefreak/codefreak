package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class Evaluation(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var answer: Answer,

  /**
   * Link to the requirement that has been used to evaluate the answer
   */
  @ManyToOne
  var requirement: Requirement,

  /**
   * The result value that was determined by checking the requirements
   */
  var result: Long
) : BaseEntity()
