package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.GradeDefinition
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*


/**
 * Repository of GradeDefinition. Associated generic functions added
 */
@Repository
interface GradeDefinitionRepository : CrudRepository<GradeDefinition, UUID> {

  fun findByEvaluationStepDefinitionId(id : UUID) : Optional<GradeDefinition>
  fun findByPointsOfEvaluationStepId(id: UUID) : Optional<GradeDefinition>
  fun findByEvaluationStep(step: EvaluationStep) : Optional<GradeDefinition>

}
