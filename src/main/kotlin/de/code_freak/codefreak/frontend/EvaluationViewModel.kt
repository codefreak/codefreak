package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationStep
import de.code_freak.codefreak.service.evaluation.EvaluationService
import org.slf4j.LoggerFactory
import java.util.UUID

data class EvaluationViewModel(
  val evaluation: Evaluation,
  val resultContents: Map<UUID, Any>,
  val resultTemplates: Map<UUID, String>
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun create(evaluation: Evaluation, evaluationService: EvaluationService, summary: Boolean = false): EvaluationViewModel {
      val resultTemplates = mutableMapOf<UUID, String>()
      val resultContents = mutableMapOf<UUID, Any>()
      val templatePrefix = if (summary) "evaluation-summary/" else "evaluation/"
      evaluation.evaluationSteps.forEach {
        if (it.result == EvaluationStep.EvaluationStepResult.ERRORED) {
          resultContents[it.id] = it.summary as String
          resultTemplates[it.id] = templatePrefix + "error"
        } else {
          try {
            resultContents[it.id] = it.summary as String
            resultTemplates[it.id] = templatePrefix + it.runnerName
          } catch (e: Exception) {
            log.error(e.message)
            resultContents[it.id] = if (summary) "Error determining result of ${it.runnerName} runner"
              else "Error while displaying result"
            resultTemplates[it.id] = templatePrefix + "error"
          }
        }
      }
      return EvaluationViewModel(evaluation, resultContents, resultTemplates)
    }
  }
}
