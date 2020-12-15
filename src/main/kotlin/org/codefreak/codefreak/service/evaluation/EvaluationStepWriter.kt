package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.service.EvaluationFinishedEvent
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EvaluationStepWriter : ItemWriter<EvaluationStep> {

  @Autowired
  private lateinit var evaluationService: EvaluationService

  @Transactional
  override fun write(items: MutableList<out EvaluationStep>) {
    for (step in items) {
      step.status = EvaluationStep.EvaluationStepStatus.FINISHED
      evaluationService.saveEvaluation(step.evaluation)
    }
  }
}
