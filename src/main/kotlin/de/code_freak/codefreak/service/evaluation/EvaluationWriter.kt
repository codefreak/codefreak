package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.repository.EvaluationResultRepository
import de.code_freak.codefreak.service.EvaluationFinishedEvent
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EvaluationWriter : ItemWriter<Evaluation> {

  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var evaluationRepository: EvaluationRepository

  @Autowired
  private lateinit var evaluationResultRepository: EvaluationResultRepository

  @Transactional
  override fun write(items: MutableList<out Evaluation>) {
    items.forEach {
      evaluationResultRepository.saveAll(it.results)
      evaluationRepository.save(it)
      eventPublisher.publishEvent(EvaluationFinishedEvent(it))
    }
  }
}
