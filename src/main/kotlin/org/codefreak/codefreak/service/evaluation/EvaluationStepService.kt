package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.EvaluationStatusUpdatedEvent
import org.codefreak.codefreak.service.EvaluationStepStatusUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationStepService {
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var stepRepository: EvaluationStepRepository

  @Autowired
  private lateinit var gradeDefinitionService: GradeDefinitionService

  @Autowired
  private lateinit var poeStepService: PointsOfEvaluationStepService

  @Autowired
  private lateinit var gradeService: GradeService

  @Autowired
  private lateinit var runnerService: EvaluationRunnerService

  fun getEvaluationStep(stepId: UUID): EvaluationStep {
    return stepRepository.findById(stepId).orElseThrow {
      EntityNotFoundException("EvaluationStep $stepId could not be found")
    }
  }

  /**
   * Determine if an evaluation step has to be executed by a runner
   */
  fun stepNeedsExecution(step: EvaluationStep): Boolean {
    if (!runnerService.isAutomated(step.definition.runnerName)) {
      return false
    }
    // non-finished steps or errored steps can be executed again
    return step.status != EvaluationStepStatus.FINISHED || step.result === EvaluationStepResult.ERRORED
  }

  @Transactional
  fun updateEvaluationStepStatus(stepId: UUID, status: EvaluationStepStatus) {
    updateEvaluationStepStatus(getEvaluationStep(stepId), status)
  }

  @Transactional
  fun updateEvaluationStepStatus(step: EvaluationStep, status: EvaluationStepStatus) {
    val evaluation = step.evaluation
    val originalEvaluationStatus = evaluation.stepStatusSummary
    step.status = status
    val newEvaluationStatus = evaluation.stepStatusSummary
    // The regular Evaluation keeps the Comment EvaluationStep on Pending until the teacher writes a comment.
    // So we need every Step finished except one, because the Comment Step is fixed and cant be removed.
    var count = 0
    for (evalStep in evaluation.evaluationSteps) {
      if (evalStep.status != EvaluationStepStatus.FINISHED) {
        count++
      }
    }
    // If the count is at least one, we can assume that only the Comment EvaluationStep is on Pending.
    // If all other steps are finished, calc a grade for the given evaluation
    // evaluation.stepStatusSummary == EvaluationStepStatus.FINISHED
    if (count <= 1) {
      gradeService.createOrUpdateGradeFromEvaluation(evaluation)
    }
    eventPublisher.publishEvent(EvaluationStepStatusUpdatedEvent(step, status))
    // check if status of evaluation has changed
    if (originalEvaluationStatus !== newEvaluationStatus) {
      eventPublisher.publishEvent(EvaluationStatusUpdatedEvent(evaluation, newEvaluationStatus))
    }
  }

  /**
   * Add a new evaluation step to the evaluation based on a given definition.
   * If a step with the same definition already exists, it will be replaced.
   */
  fun addStepToEvaluation(evaluation: Evaluation, stepDefinition: EvaluationStepDefinition): EvaluationStep {
    // remove existing step with this definition from evaluation
    evaluation.evaluationSteps.removeIf { it.definition == stepDefinition }
    return EvaluationStep(stepDefinition, evaluation, EvaluationStepStatus.PENDING).also {
      evaluation.addStep(it)
    }
  }

  fun saveEvaluationStep(step: EvaluationStep) = stepRepository.save(step)

  /**
   * Configure a EvaluationStep and prepare it for Autograding.
   * Adds missing relations and puts them together
   */
  fun configureEvaluationStepForAutoGrading(step: EvaluationStep): EvaluationStep {
    step.gradeDefinition = gradeDefinitionService.findByEvaluationStepDefinition(step.definition.id)
    val updatedStep = stepRepository.save(step)

    // Check Consistency. Might throw errors due entity not persistent (lazy load)
    if (updatedStep.gradeDefinition == null) {
      updatedStep.gradeDefinition = gradeDefinitionService.findByEvaluationStep(updatedStep)
      stepRepository.save(updatedStep)
      // Create a PointsOfEvaluationStep Relation between this EvaluationStep and the recent obtained gradeDefinition
      if (updatedStep.pointsOfEvaluationStep == null) {
        poeStepService.save(PointsOfEvaluationStep(evaluationStep = updatedStep, updatedStep.gradeDefinition!!))
      }
    }
    // returns the updatedStep. If it contains a gradeDefinition it will be considered for Autograding. If not it will be ignored.
    return updatedStep
  }

  /**
   * Starts an possible autograding process. If requirements are met a grade might be calculated.
   */
  fun startAutograding(step: EvaluationStep) {
    val updatedStep = configureEvaluationStepForAutoGrading(step)
    // Only Autograde if a gradeDefinition is present. Otherwise this Task is not eligible for Autograding
    updatedStep.gradeDefinition?.let {
        if (it.active) {
          poeStepService.calculate(step)
        }
      }
  }

  /**
   * This function should be called if a teacher has edited an invalid PointsOfEvaluation
   * If he set up points from zero the and the pointsOfEvaluationStep is still errored
   * it will become failed.
   */
  fun updateResultFromPointsOfEvaluationStep(poe: PointsOfEvaluationStep): Boolean {
    return if (poe.edited) {
      if (poe.reachedPoints == poe.gradeDefinition.maxPoints) {
        poe.evaluationStep.result = EvaluationStepResult.SUCCESS
      } else {
        poe.evaluationStep.result = EvaluationStepResult.FAILED
      }
      stepRepository.save(poe.evaluationStep)
      true
    } else {
      false
    }
  }
}
