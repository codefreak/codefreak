package org.codefreak.codefreak.repository

import org.codefreak.codefreak.entity.Evaluation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface EvaluationRepository : CrudRepository<Evaluation, UUID> {
  fun findFirstByAnswerIdOrderByCreatedAtDesc(answerId: UUID): Optional<Evaluation>
  fun findFirstByAnswerIdAndFilesDigest(answerId: UUID, digest: ByteArray): Optional<Evaluation>
}
