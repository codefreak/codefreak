package org.codefreak.codefreak.repository

import java.util.UUID
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.springframework.data.repository.CrudRepository

interface EvaluationStepDefinitionRepository : CrudRepository<EvaluationStepDefinition, UUID>
