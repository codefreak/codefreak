package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.*
import java.util.UUID
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

  @Autowired
  private lateinit var gradeDefinitionService: GradeDefinitionService

  @Autowired
  private lateinit var poeStepService: PointsOfEvaluationStepService

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

  fun saveEvaluationStep(step: EvaluationStep) = stepRepository.save(step)

  /**
   * Configure a EvaluationStep and prepare it for Autograding.
   * Adds missing relations and puts them together
   */
  fun configureEvaluationStepForAutoGrading(step : EvaluationStep) : EvaluationStep{
    step.gradeDefinition = gradeDefinitionService.findByEvaluationStepDefinition(step.definition.id)
    var updatedStep = stepRepository.save(step)

    //Check Consistency. Might throw errors due entity not persistent (lazy load)
    if(updatedStep.gradeDefinition==null){
      updatedStep.gradeDefinition = gradeDefinitionService.findByEvaluationStep(updatedStep)
      stepRepository.save(updatedStep)
      //Create a PointsOfEvaluationStep Relation between this EvaluationStep and the recent obtained gradeDefinition
      if(updatedStep.pointsOfEvaluationStep==null){
        poeStepService.save(PointsOfEvaluationStep(evaluationStep = updatedStep, updatedStep.gradeDefinition!!))
      }
    }
    //returns the updatedStep. If it contains a gradeDefinition it will be considered for Autograding. If not it will be ignored.
    return updatedStep
  }

  /**
   * Starts an possible autograding process. If requirements are met a grade might be calculated.
   */
  fun startAutograding(step: EvaluationStep){
    val updatedStep = configureEvaluationStepForAutoGrading(step)
    //Only Autograde if a gradeDefinition is present. Otherwise this Task is not eligible for Autograding
    updatedStep.gradeDefinition?.let {
        if(it.active){
          poeStepService.calculate(step)
        }
      }
  }

  /**
   * This function should be called if a teacher has edited an invalid PointsOfEvaluation
   * If he set up points from zero the and the pointsOfEvaluationStep is still errored
   * it will become failed.
   */
  fun updateResultFromPointsOfEvaluationStep(poe : PointsOfEvaluationStep) : Boolean {
    return if(poe.edited){
      if(poe.pOfE==poe.gradeDefinition.pEvalMax){
        poe.evaluationStep.result = EvaluationStepResult.SUCCESS
      }else{
        poe.evaluationStep.result = EvaluationStepResult.FAILED
      }
      stepRepository.save(poe.evaluationStep)
      true
    }else{
      false
    }
  }
}
