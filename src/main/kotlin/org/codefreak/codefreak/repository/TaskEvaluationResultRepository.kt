package org.codefreak.codefreak.repository

import java.util.UUID
import org.codefreak.codefreak.entity.Evaluation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskEvaluationResultRepository : CrudRepository<Evaluation, UUID>
