package org.codefreak.codefreak.repository

import java.util.Optional
import java.util.UUID
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EvaluationRepository : CrudRepository<Evaluation, UUID> {
  fun findFirstByAnswerIdOrderByCreatedAtDesc(answerId: UUID): Optional<Evaluation>
  fun findFirstByAnswerIdAndFilesDigestOrderByCreatedAtDesc(answerId: UUID, digest: ByteArray): Optional<Evaluation>

  /**
   * function to receive the Evaluation from its EvaluationStep.
   * There is a case of Backtracking in the process of automatic grade calculation
   */
  fun findByEvaluationSteps(evaluationStep: EvaluationStep): Optional<Evaluation>
}
