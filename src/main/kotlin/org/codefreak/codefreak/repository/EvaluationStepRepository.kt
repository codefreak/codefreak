package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.GradeDefinition
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EvaluationStepRepository : CrudRepository<EvaluationStep, UUID>{
  fun findAllByEvaluation(evaluation: Evaluation) : MutableList<EvaluationStep>
  fun findAllByGradeDefinition(gradeDefinition : GradeDefinition) : MutableList<EvaluationStep>
}
