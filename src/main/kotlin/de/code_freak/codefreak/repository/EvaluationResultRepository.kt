package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.EvaluationResult
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface EvaluationResultRepository : CrudRepository<EvaluationResult, UUID>
