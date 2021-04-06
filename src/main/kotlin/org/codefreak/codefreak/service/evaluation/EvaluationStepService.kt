package org.codefreak.codefreak.service.evaluation

import java.time.Instant
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
  private lateinit var gradingDefinitionService: GradingDefinitionService

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
    // We do not check if the step is already in the given status on purpose.
    // This allows updating the finishedAt timestamp (used for comments).
    when (status) {
      EvaluationStepStatus.QUEUED -> step.queuedAt = Instant.now()
      EvaluationStepStatus.FINISHED -> step.finishedAt = Instant.now()
      else -> Unit // all other status do not have a timestamp atm
    }
    val evaluation = step.evaluation
    val originalEvaluationStatus = evaluation.stepStatusSummary
    step.status = status
    val newEvaluationStatus = evaluation.stepStatusSummary

    step.definition.gradingDefinition?.let {
      if (evaluation.stepStatusSummary == EvaluationStepStatus.FINISHED && it.active) {
          gradeService.gradeCalculation(evaluation)
      }
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
    // mark all manual steps as finished without a result
    // otherwise the overall evaluation status would be stuck at "pending"
    val initialStatus = when {
      !runnerService.isAutomated(stepDefinition.runnerName) -> EvaluationStepStatus.FINISHED
      else -> EvaluationStepStatus.PENDING
    }
    return EvaluationStep(stepDefinition, evaluation, initialStatus).also {
      evaluation.addStep(it)
    }
  }

  fun saveEvaluationStep(step: EvaluationStep) = stepRepository.save(step)

  /**
   * Starts a autograding process. If requirements are met  points will be calculated.
   */
  fun startAutograding(evaluationStep: EvaluationStep) {
    // Only Autograde if a GradeDefinition is present. Otherwise this EvaluationStep is not eligible for Autograding
    evaluationStep.definition.gradingDefinition?.let {
      // Check if this step is ready to get calculated.
      if (it.active) {

        if (evaluationStep.points == null) {
          // create a PointsOfEvaluation Entity if it is not existent
          evaluationStep.points = PointsOfEvaluationStep(evaluationStep)
          // keep the gradingDefinition Entity in evaluationStep to find this evaluationStep by an existing GradingDefinition
          // Important to recalculate Points if something on the GradingDefinition fails.
          evaluationStep.gradingDefinition = it
          // start grading with the updated evaluationStep
          poeStepService.calculate(stepRepository.save(evaluationStep), it)
        }
        // start grading with the existing pointsOfEvaluationStep
        poeStepService.calculate(evaluationStep, it)
      }
    }
  }

  /**
   * This function should be called if a teacher has edited an invalid PointsOfEvaluationStep
   * If the teacher set points from zero upwards and the pointsOfEvaluationStep is still errored
   * it will turn to FAILED and considered for a Grade.
   */
  fun updateResultFromPointsOfEvaluationStep(poe: PointsOfEvaluationStep): Boolean {
    return if (poe.edited) {
      poe.evaluationStep.gradingDefinition?.let {
        if (poe.reachedPoints == it.maxPoints) {
          poe.evaluationStep.result = EvaluationStepResult.SUCCESS
        } else {
          poe.evaluationStep.result = EvaluationStepResult.FAILED
        }
        stepRepository.save(poe.evaluationStep)
        true
      }
      false
    } else {
      false
    }
  }
}
