package org.codefreak.codefreak.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
class GradeDefinition(
  /**
   * Lets enhance every EvaluationStepDefinition with some GradeDefinition
   */
  @OneToOne(mappedBy = "gradeDefinition")
  var evaluationStepDefinition: EvaluationStepDefinition

) : BaseEntity() {

  @OneToMany(mappedBy = "gradeDefinition", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  var pointsOfEvaluationStep: MutableList<PointsOfEvaluationStep> = mutableListOf()

  /**
   * boolean for UI-Slider. Will be set to true if Autograding is wanted for a task
   */
  var active: Boolean = false
  /**
   * Maximum of points reachable by its Grade definition
   */
  var pEvalMax = 10f

  /**
   * Point reduction on minor errors
   */
  var bOnMinor = 0f

  /**
   * Point reduction on major errors
   */
  var bOnMajor = 0f

  /**
   * Point reduction on Critical Errors.
   */
  var bOnCritical = 0f

  /**
   * A Grade definition is valid for many EvaluationSteps.
   */
  @OneToMany(mappedBy = "gradeDefinition", cascade = [CascadeType.ALL], orphanRemoval = true)
  var evaluationStep: MutableList<EvaluationStep> = mutableListOf()
}
