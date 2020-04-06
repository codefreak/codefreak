package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.Evaluation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TaskEvaluationResultRepository : CrudRepository<Evaluation, UUID>
