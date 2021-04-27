package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.GradingDefinition
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.repository.EvaluationStepRepository
import org.codefreak.codefreak.repository.GradingDefinitionRepository
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GradingDefinitionService : BaseService() {

  @Autowired
  private lateinit var gradingDefinitionRepository: GradingDefinitionRepository

  @Autowired
  private lateinit var evaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Autowired
  private lateinit var evaluationStepRepository: EvaluationStepRepository

  @Autowired
  private lateinit var pointsOfEvaluationStepService: PointsOfEvaluationStepService

  @Autowired
  private lateinit var gradeService: GradeService

  @Autowired
  private lateinit var evaluationStepsRepository: EvaluationStepRepository

  fun save(gradingDefinition: GradingDefinition): GradingDefinition = gradingDefinitionRepository.save(gradingDefinition)

  @Transactional
  fun findGradingDefinition(gradingDefinitionId: UUID): GradingDefinition {
    return gradingDefinitionRepository.findById(gradingDefinitionId).orElseThrow {
      EntityNotFoundException("GradingDefinition $gradingDefinitionId could not be found")
    }
  }

  @Transactional
  fun findGradingDefinitionByEvaluationStepDefinitionId(evaluationStepDefinitionId: UUID): GradingDefinition {
    return gradingDefinitionRepository.findByEvaluationStepDefinitionId(evaluationStepDefinitionId).orElseThrow {
      EntityNotFoundException("GradingDefinition by evaluationStepDefinitionId $evaluationStepDefinitionId could not be found")
    }
  }

  fun findByEvaluationStepDefinition(evaluationStepDefinitionId: UUID): GradingDefinition {
    return evaluationStepDefinitionRepository.findById(evaluationStepDefinitionId).orElseThrow {
      EntityNotFoundException("GradingDefinition by evaluationStepDefinitionId $evaluationStepDefinitionId could not be found")
    }.gradingDefinition!!
  }

  fun updateGradingDefinition(gradingDefinition: GradingDefinition, active: Boolean?, maxPoints: Float?, minorMistakePenalty: Float?, majorMistakePenalty: Float?, criticalMistakePenalty: Float?): GradingDefinition {
    active?.let { gradingDefinition.active = active }
    maxPoints?.let { gradingDefinition.maxPoints = maxPoints }
    minorMistakePenalty?.let { gradingDefinition.minorMistakePenalty = minorMistakePenalty }
    majorMistakePenalty?.let { gradingDefinition.majorMistakePenalty = majorMistakePenalty }
    criticalMistakePenalty?.let { gradingDefinition.criticalMistakePenalty = criticalMistakePenalty }
    val updatedGradingDefinition = save(gradingDefinition)
    // Set on active and recalculate points and afterwards the related grades
    if (updatedGradingDefinition.active && pointsOfEvaluationStepService.recalculatePoints(updatedGradingDefinition)) {
      val stepsList = evaluationStepsRepository.findAllByGradingDefinition(updatedGradingDefinition)
      stepsList.forEach {
        gradeService.gradeCalculation(it.evaluation)
      }
    }
    return updatedGradingDefinition
  }

  @Transactional
  fun findByPointsOfEvaluationStepId(pointsOfEvaluationStepId: UUID): GradingDefinition {
    val step = evaluationStepRepository.findByPointsId(pointsOfEvaluationStepId).get()
    return step.definition.gradingDefinition!!
  }
}
