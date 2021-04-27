package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.entity.GradingDefinition
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.repository.FeedbackRepository
import org.codefreak.codefreak.repository.PointsOfEvaluationStepsRepository
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.util.orNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PointsOfEvaluationStepService : BaseService() {

  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  @Autowired
  private lateinit var poeRepository: PointsOfEvaluationStepsRepository

  @Autowired
  private lateinit var evaluationStepsRepository: EvaluationStepRepository

  @Autowired
  private lateinit var feedbackRepository: FeedbackRepository

  @Autowired
  private lateinit var esService: EvaluationStepService

  @Autowired
  private lateinit var gradeService: GradeService

  /**
   * Check if a EvaluationStep already has a PointsOfEvaluation
   * If it misses create one if a GradingDefinition is provided and return it.
   */
  fun findOrCreateByEvaluationStep(evaluationStep: EvaluationStep): PointsOfEvaluationStep {
    poeRepository.findByEvaluationStep(evaluationStep).let { optional ->
      // If a PointsOfEvaluationStep exists, it will be returned. Otherwise it will be created.
      // If the creation failed due to a non-existent gradeDefinition it will return null
      return if (optional.isPresent) {
        optional.get()
      } else {
        val points = PointsOfEvaluationStep(evaluationStep)
        poeRepository.save(points)
      }
    }
  }

  fun findEvaluationStepById(evaluationStepId: UUID): PointsOfEvaluationStep? {
    return poeRepository.findByEvaluationStepId(evaluationStepId).orNull()
  }

  /**
   * starts calculation dependent of its associated EvaluationStepResult
   */
  fun calculate(evaluationStep: EvaluationStep, gradingDefinition: GradingDefinition) {
    when (evaluationStep.result) {
      EvaluationStepResult.SUCCESS -> onEvaluationStepResultSuccess(evaluationStep)
      EvaluationStepResult.FAILED -> onEvaluationStepResultSuccess(evaluationStep) // Same path as success but differs the result.
      EvaluationStepResult.ERRORED -> LOG.error("EvaluationStepResult = ${EvaluationStepResult.values()}, calculation in PointsOfEvaluationStepService not possible.")
    }
  }

  /**
   * This function generates partial points of a grade in the PointsOfEvaluationStep Entity which relates to the passed
   * EvaluationStep.
   * The function interrupts if there is no instance of a PointsOfEvaluationStep or a teacher set the points manually.
   * After loading the gradeDefinition and finding all Feedbacks who are not successful this function passes the
   * gradeDefinition and failed Feedbacks
   * to the collectMistake function. Afterwards mistakes are subtracted from the possible maximum points provided by
   * the gradeDefinition and save the result.
   */
  private fun onEvaluationStepResultSuccess(evaluationStep: EvaluationStep) {
    // Load or Create pointsOfEvaluationStep
    val poe = findOrCreateByEvaluationStep(evaluationStep)

    // return if the edited attribute is true. This means a teacher manually changed the points.
    if (poe.edited) {
      return
    }
    poe.evaluationStep.gradingDefinition?.let {
      // flag set to true because at this stage the EvaluationStep didnt fail in the process
      // This is necessary to let the teacher handle this evaluationStep before a grade will be calculated.
      poe.evaluationStepResultCheck = true
      // Pick all Feedbacks who are not successful and collect mistakes
      val failedFeedbacks = feedbackRepository.findByEvaluationStepAndStatusNot(evaluationStep, Feedback.Status.SUCCESS)
      val mistakePoints = collectMistakes(failedFeedbacks, it)
      poe.mistakePoints = mistakePoints
      if (mistakePoints >= it.maxPoints) {
        poe.reachedPoints = 0f
      } else {
        poe.reachedPoints = it.maxPoints - mistakePoints
      }
      // caculation successful and save.
      poe.calculationCheck = true
      poeRepository.save(poe)
    }
  }

  /**
   * Collects all mistakePoints provided by Not Successful Feedbacks with a GradeDefinition
   */
  fun collectMistakes(finalList: List<Feedback>, gradingDefinition: GradingDefinition): Float {
    var mistakePoints = 0f
    for (f in finalList) {
      if (f.isFailed) {
        when (f.severity) {
          Feedback.Severity.MINOR -> mistakePoints += gradingDefinition.minorMistakePenalty
          Feedback.Severity.MAJOR -> mistakePoints += gradingDefinition.majorMistakePenalty
          Feedback.Severity.CRITICAL -> mistakePoints += gradingDefinition.criticalMistakePenalty
          Feedback.Severity.INFO -> mistakePoints += 0f
        }
      }
    }
    return mistakePoints
  }

  /**
   * If a GradeDefinition receives an Upgrade, all related evaluationSteps will update their respective
   * PointsOfEvaluationStep and recalculate the related grade
   */
  fun recalculatePoints(gradingDefinition: GradingDefinition): Boolean {
    if (gradingDefinition.active) {
      val stepList = evaluationStepsRepository.findAllByGradingDefinition(gradingDefinition)
        if (stepList.size> 0) {
          for (step in stepList) {
            calculate(step, gradingDefinition)
          }
        return true
      }
    }
    return false
  }

  fun findById(id: UUID): PointsOfEvaluationStep {
    return poeRepository.findById(id).orElseThrow {
      EntityNotFoundException("PointsOfEvaluationStep by $id could not be found")
    }
  }

  fun save(poe: PointsOfEvaluationStep): PointsOfEvaluationStep = poeRepository.save(poe)

  fun delete(pointsOfEvaluationStep: PointsOfEvaluationStep) {
    poeRepository.delete(pointsOfEvaluationStep)
  }

  /**
   * Checks what is transmitted and updated values Save and recalculate the related grade
   */
  fun updatePointsOfEvaluationStep(poe: PointsOfEvaluationStep, reachedPoints: Float?, mistakePoints: Float?, calculationCheck: Boolean?, edited: Boolean?, evaluationStepResultCheck: Boolean?): PointsOfEvaluationStep {
    reachedPoints?.let { poe.reachedPoints = reachedPoints }
    mistakePoints?.let { poe.mistakePoints = mistakePoints }
    calculationCheck?.let { poe.calculationCheck = calculationCheck }
    edited?.let { poe.edited = edited }
    evaluationStepResultCheck?.let { poe.evaluationStepResultCheck = evaluationStepResultCheck }
    val updatedPoE = save(changeEdited(poe))
    gradeService.gradeCalculation(updatedPoE.evaluationStep.evaluation)

    return updatedPoE
  }

  /**
   * Only Autograding will set calcCheck to true. Autograding fails if the evaluationStep has errored / failed
   * A teacher can turn this state around by manually looking at the grade and adjusting the points which is zero
   * This will turn the attribute poe.edited to true.
   *
   */
  fun changeEdited(poe: PointsOfEvaluationStep): PointsOfEvaluationStep {
    if (poe.edited) {
      // Check if operation was successful -> proceed to adjust the calcCheck to prevent further adjustments of the EvaluationStep Result
      if (esService.updateResultFromPointsOfEvaluationStep(poe)) {
        poe.calculationCheck = true
      }
    }
    return poe
  }
}
