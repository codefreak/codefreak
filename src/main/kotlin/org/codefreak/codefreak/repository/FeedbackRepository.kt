package org.codefreak.codefreak.repository

import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.Feedback
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FeedbackRepository : CrudRepository<Feedback, UUID> {

  /**
   * function to receive all flawed feedbacks of an evaluationstep
   * Required to identify errors and to evaluate
   */
  fun findByEvaluationStepAndStatusAndSeverityNot(evaluationStep: EvaluationStep, status: Feedback.Status, severity: Feedback.Severity): MutableList<Feedback>

  /**
   * function to receive all EvaluationSteps which dont have a specific status. E. g. if we ask for Not Status.Info
   * we would get all flawed Feedbacks for this EvaluationStep. Used for PointsOfEvaluationStep value calculation
   */
  fun findByEvaluationStepAndStatusNot(evaluationStep: EvaluationStep, status: Feedback.Status): MutableList<Feedback>
}
