package org.codefreak.codefreak.entity

import javax.persistence.*


/**
 * Hat die Beziehung zu einer Task. Außerdem zu PointsOfEvaluation.
 * ein Assignment besteht aus mehreren Tasks. So trägt die Task evaluationstep Definitionen.
 *
 */
@Entity
class PointsOfEvaluationStep(

  /**
   * The Evaluation of which this PointsOfEvaluation belongs to
   */
  @OneToOne
  @JoinColumn(name = "evaluationStep", referencedColumnName = "id")
  var evaluationStep: EvaluationStep,

  /**
   * Gradedefinition reference to calculate and in particular update points of evaluation
   * Especially if values might have changed.
   */
  @ManyToOne
  var gradeDefinition : GradeDefinition,

  /**
   *
   */
  @ManyToOne
  var grade : Grade?=null


) : BaseEntity(){

  /**
   * A Boolean value which purpose is to make a teacher set points manually and prevent further autograding from updating the bOfT value
   *
   */
  var edited : Boolean=false

  /**
   * somekind of lever if there was any calculation done. There might not run an evaluation and therefor no errors appear.
   * Switch to true if run for the first time. If it stays false on grade Calculation it will set bOfT to the maximum of
   * points this evaluationstep will give. It also will provide a sign for the teacher that something went wrong.
   */
  var calcCheck : Boolean=false

  /**
   * A Check if a EvaluationStepResult is fine or not. This is important to check if an error was caused by us or the student
   * In any case the Result might not be succeded the teacher has to take action.
   * It also provides as check if a grade will be calculated in the first place.
   *
   * a change of this boolean value has to trigger a possivle grade-calculation. A Gradecalculation will take place if all related Evaluationsteps succeded.
   */
  var resultCheck : Boolean = false


  /**
   * sum of all mistakes refered to all flawed feedbacks
   */
  var bOfT : Float=0f


  /**
   * Points reached in this Evaluation.
   */

  var pOfE : Float=0f

}
