package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.service.EntityNotFoundException
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ParseException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@StepScope
class EvaluationStepReader : ItemReader<EvaluationStep> {

  @Autowired
  private lateinit var evaluationStepService: EvaluationStepService

  @Value("#{jobParameters['evaluationStepId']}")
  private lateinit var evaluationStepId: String

  var hasBeenRead = false

  override fun read(): EvaluationStep? {
    // we have to return null to indicate there is nothing more to read from this "batch"
    if (hasBeenRead) return null
    hasBeenRead = true
    try {
      return evaluationStepService.getEvaluationStep(UUID.fromString(evaluationStepId))
    } catch (e: EntityNotFoundException) {
      throw ParseException("EvaluationStep not found", e)
    }
  }
}
