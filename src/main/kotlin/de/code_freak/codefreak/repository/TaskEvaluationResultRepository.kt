package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.TaskEvaluationResult
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TaskEvaluationResultRepository : CrudRepository<TaskEvaluationResult, UUID>
