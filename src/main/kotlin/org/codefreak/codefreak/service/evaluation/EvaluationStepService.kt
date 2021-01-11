package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.EvaluationStatusUpdatedEvent
import org.codefreak.codefreak.service.EvaluationStepStatusUpdatedEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationStepService {

  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var stepRepository: EvaluationStepRepository

  fun getEvaluationStep(stepId: UUID): EvaluationStep {
    return stepRepository.findById(stepId).orElseThrow {
      EntityNotFoundException("EvaluationStep $stepId could not be found")
    }
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
    eventPublisher.publishEvent(EvaluationStepStatusUpdatedEvent(step, status))
    // check if status of evaluation has changed
    if (originalEvaluationStatus !== newEvaluationStatus) {
      eventPublisher.publishEvent(EvaluationStatusUpdatedEvent(evaluation, newEvaluationStatus))
    }
  }

  /**
   * Get the existing evaluation step from the evaluation or create a new one
   */
  fun addPendingEvaluationStep(evaluation: Evaluation, stepDefinition: EvaluationStepDefinition): EvaluationStep {
    // remove existing step with this definition from evaluation
    evaluation.evaluationSteps.removeIf { it.definition == stepDefinition }
    return EvaluationStep(stepDefinition, evaluation, EvaluationStepStatus.PENDING).also {
      evaluation.addStep(it)
    }
  }

  fun saveEvaluationStep(step: EvaluationStep) {
    stepRepository.save(step)
  }
}
