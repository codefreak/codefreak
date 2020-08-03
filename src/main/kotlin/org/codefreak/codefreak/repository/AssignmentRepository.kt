package org.codefreak.codefreak.repository

import java.time.Instant
import java.util.UUID
import org.codefreak.codefreak.entity.Assignment
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AssignmentRepository : CrudRepository<Assignment, UUID> {
  fun findByOwnerId(ownerId: UUID): Iterable<Assignment>

  @Query("SELECT a FROM Assignment a WHERE a.openFrom >= :after OR a.deadline >= :after")
  fun getByOpenFromAfterOrDeadlineAfter(after: Instant): Iterable<Assignment>
}
