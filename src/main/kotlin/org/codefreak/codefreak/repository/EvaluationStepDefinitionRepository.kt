package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface EvaluationStepDefinitionRepository : CrudRepository<EvaluationStepDefinition, UUID>
