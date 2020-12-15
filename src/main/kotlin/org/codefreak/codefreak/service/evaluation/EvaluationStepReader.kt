package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.repository.EvaluationStepRepository
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
  private lateinit var evaluationStepRepository: EvaluationStepRepository

  @Value("#{jobParameters['evaluationStepId']}")
  private lateinit var evaluationStepId: String

  var hasBeenRead = false

  override fun read(): EvaluationStep? {
    // we have to return null to indicate there is nothing more to read from this "batch"
    if (hasBeenRead) return null
    hasBeenRead = true
    return evaluationStepRepository.findById(UUID.fromString(evaluationStepId)).orElseThrow {
      ParseException("EvaluationStep not found")
    }
  }
}
