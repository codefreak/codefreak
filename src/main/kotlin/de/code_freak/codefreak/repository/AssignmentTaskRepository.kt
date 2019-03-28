package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.AssignmentTask
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AssignmentTaskRepository : CrudRepository<AssignmentTask, UUID>
