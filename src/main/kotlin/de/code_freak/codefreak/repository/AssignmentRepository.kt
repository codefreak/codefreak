package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.Assignment
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AssignmentRepository : CrudRepository<Assignment, UUID> {
  fun findByOwnerId(ownerId: UUID): Iterable<Assignment>
}
