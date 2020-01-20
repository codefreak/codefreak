package de.code_freak.codefreak.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * Each Evaluation is made up of several EvaluationSteps that ar running in parallel.
 * The EvaluationSteps generate Feedback
 */
@Entity
class EvaluationStep(
  var runnerName: String,
  // TODO: See GitHub Issue #234
  var position: Int
) : BaseEntity() {
  @ManyToOne(optional = false)
  var evaluation: Evaluation? = null

  @OneToMany(mappedBy = "evaluationStep", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  var feedback: MutableList<Feedback> = mutableListOf()

  @Enumerated(EnumType.STRING)
  var result: EvaluationStepResult? = null

  /**
   * Optional summary for the step result
   * Also useful to give more information about failures
   */
  var summary: String? = null

  fun addFeedback(feedback: Feedback) {
    this.feedback.add(feedback)
    feedback.evaluationStep = this
  }

  fun addAllFeedback(feedbackList: List<Feedback>) = feedbackList.forEach(this::addFeedback)

  enum class EvaluationStepResult { SUCCESS, FAILED, ERRORED }
}