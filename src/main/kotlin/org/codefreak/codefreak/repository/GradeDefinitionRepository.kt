package org.codefreak.codefreak.repository

import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.GradeDefinition
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository of GradeDefinition. Associated generic functions added
 */
@Repository
interface GradeDefinitionRepository : CrudRepository<GradeDefinition, UUID> {

  fun findByEvaluationStepDefinitionId(id: UUID): Optional<GradeDefinition>
  fun findByPointsOfEvaluationStepId(id: UUID): Optional<GradeDefinition>
  fun findByEvaluationStep(step: EvaluationStep): Optional<GradeDefinition>
}
