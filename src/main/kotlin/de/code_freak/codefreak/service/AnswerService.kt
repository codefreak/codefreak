package de.code_freak.codefreak.service

import de.code_freak.codefreak.repository.AnswerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnswerService : BaseService() {

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  fun getAnswerIdsForTaskIds(taskIds: Iterable<UUID>, userId: UUID) = answerRepository.findIdsForTaskIds(taskIds, userId).toMap()

  fun getAnswerIdForTaskId(taskId: UUID, userId: UUID) = answerRepository.findIdForTaskId(taskId, userId)
      .orElseThrow { EntityNotFoundException("Answer not found.") }
}
