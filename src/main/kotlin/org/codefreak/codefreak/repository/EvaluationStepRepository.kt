package org.codefreak.codefreak.repository

import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.GradingDefinition
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EvaluationStepRepository : CrudRepository<EvaluationStep, UUID> {
  fun findAllByGradingDefinition(gradingDefinition: GradingDefinition): MutableList<EvaluationStep>
  fun findByPointsId(pointsOfEvaluationStepId: UUID): Optional<EvaluationStep>
}
