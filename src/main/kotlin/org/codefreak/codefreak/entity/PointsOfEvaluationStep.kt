package org.codefreak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

/**
 * This Entity required a relation to a evaluationStep and a gradeDefinition.
 * Its purpose is to be part of a Grade and distribute points.
 *
 */
@Entity
class PointsOfEvaluationStep(

  /**
   * The Evaluation which this PointsOfEvaluation belongs to
   */
  @OneToOne
  @JoinColumn(name = "evaluationStep", referencedColumnName = "id")
  var evaluationStep: EvaluationStep,

  @ManyToOne
  var grade: Grade? = null

) : BaseEntity() {

  /**
   * A Boolean value in purpose of which is to make a teacher set points manually and prevent further Autograding from updating the mistakePoints value
   */
  var edited: Boolean = false

  /**
   * A Boolean value which switches on true if a calculation was done on this entity.
   *
   */
  var calculationCheck: Boolean = false

  /**
   * Checks if there is a valid result. A not valid result would be if the EvaluationStepStatus Errored.
   * This attribut is required to give the teacher the possibility of setting points manually if the student sends in a broken submission.
   */
  var evaluationStepResultCheck: Boolean = false

  /**
   * sum of all mistakes obtained from All Feedback which status is FAILED and severity is not INFO provided by the evaluationStep
   *
   */
  var mistakePoints: Float = 0f

  /**
   * Points reached in this Evaluation.
   */
  var reachedPoints: Float = 0f
}
