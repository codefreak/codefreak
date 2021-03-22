package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.entity.GradeDefinition
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.repository.FeedbackRepository
import org.codefreak.codefreak.repository.PointsOfEvaluationStepsRepository
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PointsOfEvaluationStepService : BaseService() {

  /**
   * Logging
   */
  val LOG = LoggerFactory.getLogger(this::class.simpleName)

  @Autowired
  private lateinit var poeRepository: PointsOfEvaluationStepsRepository

  @Autowired
  private lateinit var evaluationStepsRepository: EvaluationStepRepository

  @Autowired
  private lateinit var feedbackRepository: FeedbackRepository

  @Autowired
  private lateinit var gradeDefinitionService: GradeDefinitionService

  @Autowired
  private lateinit var esService: EvaluationStepService

  @Autowired
  private lateinit var gradeService: GradeService

  /**
   * Save Call of PointsOfEvaluationStep by EvaluationStepId
   */
  fun findByEvaluationStepId(id: UUID): PointsOfEvaluationStep? {
    evaluationStepsRepository.findById(id).let { optional ->
        if (optional.isPresent) {
          poeRepository.findByEvaluationStep(optional.get()).let { optional2 ->
            return if (optional2.isPresent) {
              optional2.get()
            } else {
              var step = optional.get()
              step.pointsOfEvaluationStep = PointsOfEvaluationStep(step, step.gradeDefinition!!)
              step = evaluationStepsRepository.save(step)
              step.pointsOfEvaluationStep!!
            }
          }
        } else {
          EntityNotFoundException("ID: $id has no valid entry in EvaluationStep")
        }
      }
    return null
  }

  /**
   * Finds a PointsOfEvaluationStep if present. Otherwise return null.
   */
  fun findByEvaluationStep(evalStep: EvaluationStep): PointsOfEvaluationStep? {
    val poe = poeRepository.findByEvaluationStep(evalStep)
    return if (poe.isPresent) {
      poe.get()
    } else {
      null
    }
  }

  /**
   * Get a save entity.
   */
  fun getEvaluationStepId(id: UUID): PointsOfEvaluationStep? {
    poeRepository.findByEvaluationStepId(id).let {
        return if (it.isPresent) it.get()
      else
        null
    }
  }


  /**
   * starts calculation dependend of its associated EvaluationStepResult
   */
  fun calculate(es: EvaluationStep) {
    LOG.info("calculate es: ${es.id}")
    es.result?.let {
      when (it) {
        EvaluationStepResult.SUCCESS -> onSuccess(es)
        EvaluationStepResult.FAILED -> onSuccess(es) // Same path as success but differs the result.
        EvaluationStepResult.ERRORED -> onErrored(es)
      }
    }
  }

  /**
   * If Result was a success
   */
  fun onSuccess(es: EvaluationStep) {
    val poe = findByEvaluationStepId(es.id) ?: return
    if (poe.edited) {
      return
    }
    poe.resultCheck = true
    if (es.gradeDefinition == null) {
      es.gradeDefinition = gradeDefinitionService.findByEvaluationStep(es)
    }

    val gradeDefinition = es.gradeDefinition!!

    val failedFeedbacks = feedbackRepository.findByEvaluationStepAndStatusNot(es, Feedback.Status.SUCCESS)
    LOG.info("We gathered ${failedFeedbacks.size} Failed Feedbacks")

    val mistakePoints = collectMistakes(failedFeedbacks, gradeDefinition)

    LOG.info("we collected $mistakePoints mistakepoints")

    poe.mistakePoints = mistakePoints
    if (mistakePoints >= gradeDefinition.maxPoints) {
      poe.reachedPoints = 0f
    } else {
      poe.reachedPoints = gradeDefinition.maxPoints - poe.mistakePoints
    }
    poe.calcCheck = true
    // print poe
    LOG.info("just calculated: ${poe.reachedPoints} points! Did ${poe.mistakePoints} mistakes. CalcCheck is set to ${poe.calcCheck} ")
    // Save poe
    poeRepository.save(poe)
  }


  /**
   * If Result Errored
   */
  fun onErrored(es: EvaluationStep) {
    // Nothings happen. Teacher has to take a look.
  }

  fun collectMistakes(finalList: List<Feedback>, gradeDefinition: GradeDefinition): Float {
    var mistakePoints = 0f
    for (f in finalList) {
      if (f.isFailed) {
        when (f.severity) {
          Feedback.Severity.MINOR -> mistakePoints += gradeDefinition.minorError
          Feedback.Severity.MAJOR -> mistakePoints += gradeDefinition.majorError
          Feedback.Severity.CRITICAL -> mistakePoints += gradeDefinition.criticalError
          else -> LOG.info("Feedback shows no errors / flaws")
        }
      }
    }
    return mistakePoints
  }

  /**
   * If a GradeDefinition receives an Upgrade, all related evaluationSteps will update their respective Points
   *
   */
  fun recalculatePoints(gradeDefinition: GradeDefinition) {
    val stepList = evaluationStepsRepository.findAllByGradeDefinition(gradeDefinition)
    for (step in stepList) {
      calculate(step)
    }
  }

  /**
   * CRUD-Functions
   */
  fun findById(id: UUID): PointsOfEvaluationStep {
    return poeRepository.findById(id).get()
  }

  fun save(poe: PointsOfEvaluationStep): PointsOfEvaluationStep = poeRepository.save(poe)

  fun create(evaluationStep: EvaluationStep): PointsOfEvaluationStep {
    return if (evaluationStep.gradeDefinition != null) {
      save(PointsOfEvaluationStep(evaluationStep, evaluationStep.gradeDefinition!!))
    } else {
      val gradeDefinition = gradeDefinitionService.findByEvaluationStep(evaluationStep)
      save(PointsOfEvaluationStep(evaluationStep, gradeDefinition))
    }
  }

  /**
   * Deletes a PointsOfEvaluationStep
   */
  fun delete(poe: PointsOfEvaluationStep): Boolean {
    return if (poeRepository.findById(poe.id).isPresent) {
      poeRepository.delete(poe)
      true
    } else {
      false
    }
  }

  /**
   * Updatefunction. Checks what is transmitted and updated values
   * Save when done
   */
  fun updatePointsOfEvaluationStep(poe: PointsOfEvaluationStep, reachedPoints: Float?, mistakePoints: Float?, calcCheck: Boolean?, edited: Boolean?, resultCheck: Boolean?): PointsOfEvaluationStep {
    reachedPoints?.let { poe.reachedPoints = reachedPoints }
    mistakePoints?.let { poe.mistakePoints = mistakePoints }
    calcCheck?.let { poe.calcCheck = calcCheck }
    edited?.let { poe.edited = edited }
    resultCheck?.let { poe.resultCheck = resultCheck }
    val updatedPoE = save(changeEdited(poe))
    gradeService.createOrUpdateGradeFromPointsOfEvaluation(updatedPoE)

    return updatedPoE
  }

  /**
   * Only Autograding will set calcCheck to true. Autograding fails if the evaluationStep has errored / failed
   * A teacher can turn this state around by manually looking at the grade and adjusting the points which should be zero
   * This will turn the attribute poe.edited to true and keep its attribute calcCheck false.
   *
   */
  fun changeEdited(poe: PointsOfEvaluationStep): PointsOfEvaluationStep {
    if (poe.edited) {
      // Check if operation was successful -> proceed to adjust the calcCheck to prevent further adjustments of the EvaluationStep Result
      if (esService.updateResultFromPointsOfEvaluationStep(poe)) {
        poe.calcCheck = true
      } else {
        LOG.error("Something went wrong in changeEvaluationStepStatus")
      }
    }
    return poe
  }
}
