package org.codefreak.codefreak.repository

import java.util.UUID
import org.codefreak.codefreak.entity.Task
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : CrudRepository<Task, UUID> {
  fun findByOwnerIdAndAssignmentIsNullOrderByCreatedAt(ownerId: UUID): Collection<Task>
}
