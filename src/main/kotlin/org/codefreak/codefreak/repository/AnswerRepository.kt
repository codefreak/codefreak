package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.Answer
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AnswerRepository : CrudRepository<Answer, UUID> {
  @Query("SELECT new kotlin.Pair(a.task.id, a.id) FROM Answer a WHERE a.submission.user.id = :userId AND a.task.id IN (:taskIds)")
  fun findIdsForTaskIds(@Param("taskIds") taskIds: Iterable<UUID>, @Param("userId") userId: UUID): Collection<Pair<UUID, UUID>>

  fun findByTaskIdAndSubmissionUserId(taskId: UUID, userId: UUID): Optional<Answer>
}
