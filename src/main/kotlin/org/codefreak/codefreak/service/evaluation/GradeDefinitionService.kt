package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.GradeDefinition
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.repository.GradeDefinitionRepository
import org.codefreak.codefreak.service.BaseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


/**
 * Service to manage GradeDefinitions. Tasks are holder of such GradeDefinition as they provide rules to handle a Grade.
 */
@Service
class GradeDefinitionService : BaseService(){

  /**
   * Logging
   */
  val log = LoggerFactory.getLogger(GradeDefinitionService::class.simpleName)!!

  /**
   * Repositories and Services used in this Service
   */
  @Autowired
  private lateinit var gradeDefinitionRepository: GradeDefinitionRepository

  @Autowired
  private lateinit var evaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Autowired
  private lateinit var pointsOfEvaluationStepService: PointsOfEvaluationStepService


  fun save(gradeDefinition: GradeDefinition) : GradeDefinition = gradeDefinitionRepository.save(gradeDefinition)

  /**
   * check if task already has a GradeDefinition and create a default one. Then return the fresh one OR an already existing one
   * Currently not in use. So uncommented
   */
//  @Transactional
//  fun getOrCreateValidGradeDefinitionByEvaluationStepDefinition(evaluationStepDefinition: EvaluationStepDefinition) : GradeDefinition {
//    return gradeDefinitionRepository.findByEvaluationStepDefinition(evaluationStepDefinition)
//      .map { if(it.evaluationStepDefinition != evaluationStepDefinition) {
//        createGradeDefinition(evaluationStepDefinition).apply { log.info("GradeDefinition with task_id $evaluationStepDefinition created and saved.") }
//      }else{ it.apply { log.info("GradeDefinition with task_id $evaluationStepDefinition already existed. Loaded and returned.") }}}
//      .orElseGet{ createGradeDefinition(evaluationStepDefinition).apply { log.info("GradeDefinition with task_id $evaluationStepDefinition created and saved.") }}
//  }

  /**
   * Creates and saves a GradeDefinition
   */
  @Transactional
  fun createGradeDefinition(evaluationStepDefinition: EvaluationStepDefinition) : GradeDefinition{
    return save(GradeDefinition(evaluationStepDefinition))
  }

  /**
   * find GradeDefinition by a specific id. Used for GraphQl
   */
  @Transactional
  fun findGradeDefinition(id: UUID): GradeDefinition {
    return gradeDefinitionRepository.findById(id).get()
  }

  /**
   * find GradeDefinition by a specific EvaluationStepDefinitionId. Used for GraphQl
   */
  @Transactional
  fun findGradeDefinitionByEvaluationStepDefinitionId(id: UUID): GradeDefinition{
    return gradeDefinitionRepository.findByEvaluationStepDefinitionId(id).get()
  }


  /**
   * find a GradeDefinition by EvaluationStepDefinition
   */
  fun findByEvaluationStepDefinition(esd : UUID) : GradeDefinition {
    return evaluationStepDefinitionRepository.findById(esd).get().gradeDefinition!!
  }

  /**
   * function to update an existing GradeDefinition
   */
  @Transactional
  fun updateGradeDefinitionValues(gradeDefinition: GradeDefinition, pEvalMax : Float?, bOnMinor: Float?, bOnMajor : Float?, bOnCritical: Float?) : GradeDefinition {
    pEvalMax?.let { gradeDefinition.pEvalMax = pEvalMax }
    bOnMinor?.let { gradeDefinition.bOnMinor = bOnMinor }
    bOnMajor?.let { gradeDefinition.bOnMajor = bOnMajor }
    bOnCritical?.let { gradeDefinition.bOnCritical = bOnCritical}
    val def = save(gradeDefinition)
    pointsOfEvaluationStepService.recalculatePoints(gradeDefinition)
    return def
  }

  /**
   * function to update an existing GradeDefinitions status
   */
  @Transactional
  fun updateGradeDefinitionStatus(gradeDefinition: GradeDefinition,active :Boolean?) : GradeDefinition {
    active?.let { gradeDefinition.active = active }
    val def = save(gradeDefinition)
    pointsOfEvaluationStepService.recalculatePoints(gradeDefinition)
    return def
  }


  /**
   * Retrieve GradeDefinition by PointsOfEvaluation
   */
  @Transactional
  fun findByPointsOfEvaluationStepId(id :UUID) : GradeDefinition =
    gradeDefinitionRepository.findByPointsOfEvaluationStepId(id).get()


  /**
   * find GradeDefinition by EvaluationStep
   */
  fun findByEvaluationStep(step : EvaluationStep) : GradeDefinition = gradeDefinitionRepository.findByEvaluationStep(step).get()


}
