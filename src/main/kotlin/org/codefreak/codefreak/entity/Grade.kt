package org.codefreak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

@Entity
class Grade(

  /**
   * Every Evaluation can have a grade. Depends if enabled or not. Later we can pick the a result
   */
  @OneToOne
  @JoinColumn(name = "evaluation", referencedColumnName = "id")
  var evaluation: Evaluation,

  /**
   * A Grade is always part of a set in answer entity.
   */
  @ManyToOne
  var answer: Answer

) : BaseEntity() {

  /**
   * Percentage of 100 how much a student reached. defined as float because value will be:  1>=x>=0
   */
  var gradePercentage: Float? = null
}
