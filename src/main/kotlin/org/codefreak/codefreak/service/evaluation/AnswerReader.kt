package org.codefreak.codefreak.service.evaluation

import java.util.UUID
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.repository.AnswerRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ParseException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@StepScope
class AnswerReader : ItemReader<Answer> {

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  @Value("#{jobParameters['answerId']}")
  private lateinit var answerId: String

  private val answerIds by lazy {
    listOf(UUID.fromString(answerId))
  }

  private var nextIndex = 0

  override fun read(): Answer? {
    return if (nextIndex >= answerIds.size) null else answerRepository.findById(answerIds[nextIndex++]).orElseThrow {
      ParseException("Answer not found")
    }
  }
}
