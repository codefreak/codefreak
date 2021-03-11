package org.codefreak.codefreak.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
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
  var gradePercentage: Float = 0f

  /**
   * Checks if a Grade is actually calculated. Otherwise a formatted zero might be shown in frontend due to a possible
   * grade applied to their respective evaluation but no grade was calculated.
   */
  var calculated: Boolean = false

  /**
   * A Grade is calculated of multiply PointsOfEvaluationSteps.
   */
  @OneToMany(mappedBy = "grade", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  var pointsOfEvaluationStep = mutableSetOf<PointsOfEvaluationStep>()
}
