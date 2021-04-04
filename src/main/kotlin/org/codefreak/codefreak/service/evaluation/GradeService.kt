package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.Grade
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.codefreak.codefreak.repository.GradeRepository
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.util.orNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GradeService : BaseService() {

  @Autowired
  private lateinit var gradeRepository: GradeRepository

  @Autowired
  private lateinit var poeStepService: PointsOfEvaluationStepService

  /**
   * function to initialize a GradeCalculation. It updates a Grade or creates one from scratch.
   */
  fun gradeCalculation(evaluation: Evaluation): Grade? {
    val stepList = evaluation.evaluationSteps

    return if (validateEvaluationSteps(stepList)) {
      val grade = findOrCreateGrade(evaluation)
      val poeList = mutableListOf<PointsOfEvaluationStep>()
      // Add grade to PointsOfEvaluationStep. Afterwards an existing grade just can collects its PointsOfEvaluationStep Child and recalculate the grade
      for (s in stepList) {
        val poe = poeStepService.findOrCreateByEvaluationStepId(s.id)
        // There might be Steps without a PointsOfEvaluationStep Entity due to deactivated autograding
        if (poe != null) {
          poe.grade = grade
          poeList.add(poeStepService.save(poe))
        }
      }
      calculateGrade(grade, poeList)
    } else {
      null
    }
  }

  /**
   * Validates all EvaluationSteps. If there are Errors this function returns false
   * Decider if a grade will be calculated.
   *
   * The validation will pass if there is just one EvaluationStep.Result Null.
   * This is currently the case because the Comment EvaluationStep.Result is NULL straight after a student has run an evaluation.
   *
   */
  private fun validateEvaluationSteps(steps: MutableSet<EvaluationStep>): Boolean {
    val updatedSteps = mutableListOf<EvaluationStep>()

    for (s in steps) {
      if (s.result != null)updatedSteps.add(s)
    }
    if (steps.size <= (updatedSteps.size + 1)) {
      for (s in updatedSteps) {
        if (s.result == null) { return false }
        s.result?.let { if (it == EvaluationStepResult.ERRORED) return false }
      }
      return true
    }
    return false
  }

  /**
   * Calculates a grade based on the points archived in a PointsOfEvaluationStep List
   * Sets grade.calculated on true at last so it will be considered for a grade.
   */
  private fun calculateGrade(grade: Grade, poeList: MutableList<PointsOfEvaluationStep>): Grade {
    var points = 0f
    var maxPoints = 0f
    for (poe in poeList) {
      // Only pick for autograding if grader is activated.
        poe.evaluationStep.definition.gradingDefinition?.let { it ->
          if (it.active) {
            points += poe.reachedPoints
            maxPoints += it.maxPoints
          }
        }
    }
    grade.gradePercentage = (100 / maxPoints * points)
    return save(grade)
  }

  /**
   * Returns the best answer if one exists.
   */
  fun getBestGradeOfAnswer(answerId: UUID): Grade? {
    return gradeRepository.findFirstByAnswerIdOrderByGradePercentageDesc(answerId).orNull()
  }

  fun save(grade: Grade) = gradeRepository.save(grade)

  /**
   * If a Grade is present pick it and return it. Otherwise create a new Grade for this evaluation / answer
   * @return New Grade or already existing one
   */
  fun findOrCreateGrade(evaluation: Evaluation): Grade {
    gradeRepository.findByEvaluation(evaluation).let {
      // Check if grade is present. If not, create one, save it and perform a calculation by its Evaluation
      return if (!it.isPresent) {
        save(Grade(evaluation, evaluation.answer))
      } else {
        it.get()
      }
    }
  }
}
