package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStep
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EvaluationStepWriter : ItemWriter<EvaluationStep?> {

  @Autowired
  private lateinit var evaluationStepService: EvaluationStepService

  @Transactional
  override fun write(items: MutableList<out EvaluationStep?>) {
    // if the application shuts down the processor might return null
    items.filterNotNull().forEach { step ->
      evaluationStepService.saveEvaluationStep(step)
    }
  }
}
