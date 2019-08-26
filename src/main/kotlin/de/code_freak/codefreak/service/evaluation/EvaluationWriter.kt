package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.repository.EvaluationRepository
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EvaluationWriter : ItemWriter<Evaluation> {

  @Autowired
  private lateinit var evaluationRepository: EvaluationRepository

  override fun write(items: MutableList<out Evaluation>) {
    evaluationRepository.saveAll(items)
  }
}
