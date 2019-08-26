package de.code_freak.codefreak.repository

import de.code_freak.codefreak.entity.Evaluation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface EvaluationRepository : CrudRepository<Evaluation, UUID> {
  fun findFirstByAnswerIdOrderByCreatedAtDesc(answerId: UUID): Optional<Evaluation>
}
