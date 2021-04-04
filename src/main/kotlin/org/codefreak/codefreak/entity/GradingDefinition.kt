package org.codefreak.codefreak.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
class GradingDefinition(

  @OneToOne(mappedBy = "gradingDefinition")
  var evaluationStepDefinition: EvaluationStepDefinition

) : BaseEntity() {

  /**
   * Will be set to true if Autograding is wanted for a task
   */
  var active: Boolean = false
  /**
   * Maximum of points reachable by its Grade definition
   */
  var maxPoints: Float = 0f

  /**
   * Point reduction on minor severity
   */
  var minorMistakePenalty: Float = 0f

  /**
   * Point reduction on major severity
   */
  var majorMistakePenalty: Float = 0f

  /**
   * Point reduction on Critical severity.
   */
  var criticalMistakePenalty: Float = 0f

  /**
   * A Grade definition is valid for many EvaluationSteps.
   */
  @OneToMany(mappedBy = "gradingDefinition", cascade = [CascadeType.ALL], orphanRemoval = true)
  var evaluationStep = mutableSetOf<EvaluationStep>()
}
