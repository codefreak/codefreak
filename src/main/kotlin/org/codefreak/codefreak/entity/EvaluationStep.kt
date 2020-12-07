package org.codefreak.codefreak.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import org.hibernate.annotations.Type

/**
 * Each Evaluation is made up of several EvaluationSteps that ar running in parallel.
 * The EvaluationSteps generate Feedback
 */
@Entity
class EvaluationStep(
  @ManyToOne(optional = false)
  var definition: EvaluationStepDefinition,

  @ManyToOne(optional = false)
  var evaluation: Evaluation
) : BaseEntity() {
  @OneToMany(mappedBy = "evaluationStep", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  var feedback = mutableSetOf<Feedback>()

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

  enum class EvaluationStepResult { SUCCESS, FAILED, ERRORED }
}
