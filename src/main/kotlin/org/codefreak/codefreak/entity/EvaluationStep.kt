package org.codefreak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.*

/**
 * Each Evaluation is made up of several EvaluationSteps that ar running in parallel.
 * The EvaluationSteps generate Feedback
 */
@Entity
class EvaluationStep(
  @ManyToOne(optional = false)
  var definition: EvaluationStepDefinition,

  @ManyToOne(optional = false)
  var evaluation: Evaluation,

  @Enumerated(EnumType.STRING)
  var status: EvaluationStepStatus
) : BaseEntity() {
  @OneToMany(mappedBy = "evaluationStep", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  var feedback = mutableSetOf<Feedback>()

  @OneToOne(mappedBy ="evaluationStep", cascade = [CascadeType.ALL])
  var pointsOfEvaluationStep : PointsOfEvaluationStep?=null

  @ManyToOne
  var gradeDefinition : GradeDefinition?=null

  @Enumerated(EnumType.STRING)
  var result: EvaluationStepResult? = null

  /**
   * Optional summary for the step result
   * Also useful to give more information about failures
   */
  @Column(length = 1048576)
  @Lob
  @Type(type = "org.hibernate.type.TextType")
  var summary: String? = null

  fun addFeedback(feedback: Feedback) {
    this.feedback.add(feedback)
    feedback.evaluationStep = this
  }

  fun addAllFeedback(feedbackList: List<Feedback>) = feedbackList.forEach(this::addFeedback)
}
