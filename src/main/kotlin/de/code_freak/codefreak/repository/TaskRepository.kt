package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.Task
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TaskRepository : CrudRepository<Task, UUID> {
  fun findByOwnerIdAndAssignmentIsNullOrderByCreatedAt(ownerId: UUID): Collection<Task>
}
