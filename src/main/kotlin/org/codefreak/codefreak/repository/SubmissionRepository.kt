package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.Submission
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SubmissionRepository : CrudRepository<Submission, UUID> {
  /**
   * Get a list of submissions for a specific assignment
   */
  fun findByAssignmentId(assignmentId: UUID): List<Submission>

  fun findByAssignmentIdAndUserId(assignmentId: UUID?, userId: UUID): Optional<Submission>

  fun findAllByUserId(userId: UUID): List<Submission>
}
