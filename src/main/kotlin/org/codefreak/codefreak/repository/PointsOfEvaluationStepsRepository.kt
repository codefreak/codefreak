package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.Grade
import org.codefreak.codefreak.entity.GradeDefinition
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*


/**
 * Repository of PointsOfEvaluation. Associated generic functions added
 *
 */
@Repository
interface PointsOfEvaluationStepsRepository : CrudRepository<PointsOfEvaluationStep,UUID>{
  fun findByEvaluationStep(evaluationStep : EvaluationStep) : Optional<PointsOfEvaluationStep>
  fun findByEvaluationStepId(id : UUID) : Optional<PointsOfEvaluationStep>
}
