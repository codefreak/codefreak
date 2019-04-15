package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.Requirement
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TaskEvaluationRepository : CrudRepository<Requirement, UUID>
