package org.codefreak.codefreak.repository

import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.PointsOfEvaluationStep
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PointsOfEvaluationStepsRepository : CrudRepository<PointsOfEvaluationStep, UUID> {
  fun findByEvaluationStep(evaluationStep: EvaluationStep): Optional<PointsOfEvaluationStep>
  fun findByEvaluationStepId(id: UUID): Optional<PointsOfEvaluationStep>
}
