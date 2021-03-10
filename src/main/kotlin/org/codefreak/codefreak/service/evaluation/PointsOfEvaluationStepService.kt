package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.*
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.repository.FeedbackRepository
import org.codefreak.codefreak.repository.PointsOfEvaluationStepsRepository
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.ArrayList


@Service
class PointsOfEvaluationStepService : BaseService(){

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
  private lateinit var esService : EvaluationStepService

  @Autowired
  private lateinit var gradeService : GradeService



  /**
   * Save Call of PointsOfEvaluationStep by EvaluationStepId
   */
  fun findByEvaluationStepId(id : UUID) : PointsOfEvaluationStep?{
    evaluationStepsRepository.findById(id).let { optional ->
        if(optional.isPresent){
          poeRepository.findByEvaluationStep(optional.get()).let { optional2 ->
            return if(optional2.isPresent){
              optional2.get()
            }else{
              var step = optional.get()
              step.pointsOfEvaluationStep=PointsOfEvaluationStep(step,step.gradeDefinition!!)
              step = evaluationStepsRepository.save(step)
              step.pointsOfEvaluationStep!!
            }
          }
        }else{
          EntityNotFoundException("ID: $id has no valid entry in EvaluationStep")
        }
      }
    return null
  }

  fun findByEvaluationStep(evalStep : EvaluationStep) : PointsOfEvaluationStep{
    val poe = poeRepository.findByEvaluationStep(evalStep)

    poe.isPresent.let { return poe.get() }
  }

  /**
   * Get a save entity.
   */
  fun getEvaluationStepId(id: UUID) : PointsOfEvaluationStep?{
    return poeRepository.findByEvaluationStepId(id).let {
      return if(it.isPresent) it.get()
      else
        null
    }
  }


  /**
   * Function to merge two lists.
   */
  fun <T> merge(first: List<T>, second: List<T>): List<T> {
    val list: MutableList<T> = ArrayList()
    list.addAll(first)
    list.addAll(second)
    return list.toSet().toMutableList()
  }

  /**
   * starts calculation in depend of its associated EvaluationStepResult
   */
  fun calculate(es : EvaluationStep){
    LOG.info("calculate es: ${es.id}")
    es.result?.let {
      when(it){
        EvaluationStepResult.SUCCESS -> onSuccess(es)
        EvaluationStepResult.FAILED -> onFailed(es) //try it. Should be the same path if flaws were found
        EvaluationStepResult.ERRORED -> onErrored(es)
      }
    }
  }

  /**
   * If Result was a success
   */
  fun onSuccess(es : EvaluationStep) {
    val poe = findByEvaluationStepId(es.id) ?: return
    if(poe.edited){
      return
    }
    poe.resultCheck = true
    if(es.gradeDefinition==null){
      es.gradeDefinition = gradeDefinitionService.findByEvaluationStep(es)
    }

    val gradeDefinition = es.gradeDefinition!!

    val failedFeedbacks = feedbackRepository.findByEvaluationStepAndStatusNot(es,Feedback.Status.SUCCESS)
    LOG.info("We gathered ${failedFeedbacks.size} Failed Feedbacks")

    var bOfT = collectMistakes(failedFeedbacks,gradeDefinition)

    LOG.info("we collected $bOfT mistakepoints")

    poe.bOfT = bOfT
    if(bOfT>=gradeDefinition.pEvalMax){
      poe.pOfE=0f
    }else{
      poe.pOfE=gradeDefinition.pEvalMax-poe.bOfT
    }
    poe.calcCheck=true
    //print poe
    LOG.info("just calculated: ${poe.pOfE} points! Did ${poe.bOfT} mistakes. CalcCheck is set to ${poe.calcCheck} ")
    //Save poe
    poeRepository.save(poe)
  }

  /**
   * if Result Failed badly
   */
  fun onFailed(es : EvaluationStep) {
    //Nothing happens here. Teacher has to take a look.
  }

  /**
   * If Result Errored
   */
  fun onErrored(es : EvaluationStep) {
    //Nothings happen. Teacher has to take a look.
  }

  fun collectMistakes(finalList : List<Feedback>, gradeDefinition : GradeDefinition) : Float {
    var bOfT = 0f
    for(f in finalList){
      if(f.isFailed){
        when(f.severity){
          Feedback.Severity.MINOR -> bOfT +=gradeDefinition.bOnMinor
          Feedback.Severity.MAJOR -> bOfT +=gradeDefinition.bOnMajor
          Feedback.Severity.CRITICAL -> bOfT +=gradeDefinition.bOnCritical
          else -> LOG.info("Feedback shows no errors / flaws")
        }
      }
    }
    return bOfT
  }

  /**
   * If a GradeDefinition receives an Upgrade, all related evaluationSteps will update their respective Points
   *
   */
  fun recalculatePoints(gradeDefinition : GradeDefinition) {
    var stepList = evaluationStepsRepository.findAllByGradeDefinition(gradeDefinition)
    for(step in stepList){
      calculate(step)
    }
  }



  /**
   * CRUD-Functions
   */
  fun findById(id : UUID) : PointsOfEvaluationStep {
    return poeRepository.findById(id).get()
  }

  fun save(poe: PointsOfEvaluationStep) : PointsOfEvaluationStep = poeRepository.save(poe)

  fun create(evaluationStep : EvaluationStep) : PointsOfEvaluationStep{
    return if(evaluationStep.gradeDefinition!=null){
      save(PointsOfEvaluationStep(evaluationStep,evaluationStep.gradeDefinition!!))
    }else{
      val gradeDefinition = gradeDefinitionService.findByEvaluationStep(evaluationStep)
      save(PointsOfEvaluationStep(evaluationStep,gradeDefinition))
    }
  }


  /**
   * Deletes a PointsOfEvaluationStep
   */
  fun delete(poe : PointsOfEvaluationStep) : Boolean{
    return if(poeRepository.findById(poe.id).isPresent){
      poeRepository.delete(poe)
      true
    }else{
      false
    }
  }

  /**
   * Updatefunction. Checks what is transmitted and updated values
   * Save when done
   */
  fun updatePointsOfEvaluationStep(poe: PointsOfEvaluationStep,pOfE : Float?, bOfT : Float?, calcCheck : Boolean?,edited : Boolean?,resultCheck : Boolean?) : PointsOfEvaluationStep{
    pOfE?.let { poe.pOfE = pOfE }
    bOfT?.let { poe.bOfT = bOfT }
    calcCheck?.let { poe.calcCheck = calcCheck }
    edited?.let { poe.edited = edited }
    resultCheck?.let { poe.resultCheck = resultCheck }
    LOG.info("processing update : ${poe.evaluationStep}")
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
  fun changeEdited(poe : PointsOfEvaluationStep) : PointsOfEvaluationStep{
    if(poe.edited){
      //Check if operation was successful -> proceed to adjust the calcCheck to prevent further adjustments of the EvaluationStep Result
      if(esService.updateResultFromPointsOfEvaluationStep(poe)){
        poe.calcCheck=true
      }else{
        LOG.error("Something went wrong in changeEvaluationStepStatus")
      }
    }
    return poe
  }

}
