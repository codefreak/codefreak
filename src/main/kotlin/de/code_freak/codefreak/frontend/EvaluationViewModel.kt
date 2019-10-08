package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.service.evaluation.EvaluationService
import org.slf4j.LoggerFactory
import java.util.UUID

data class EvaluationViewModel(val evaluation: Evaluation,
                               val resultContents: Map<UUID, Any>,
                               val resultTemplates: Map<UUID, String>) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun create(evaluation: Evaluation, evaluationService: EvaluationService): EvaluationViewModel {
      val resultTemplates = mutableMapOf<UUID, String>()
      val resultContents = mutableMapOf<UUID, Any>()
      evaluation.results.forEach {
        if (it.error) {
          resultContents[it.id] = String(it.content)
          resultTemplates[it.id] = "error"
        } else {
          try {
            resultContents[it.id] = evaluationService.getEvaluationRunner(it.runnerName).parseResultContent(it.content)
            resultTemplates[it.id] = it.runnerName
          } catch (e: Exception) {
            log.error(e.message)
            resultContents[it.id] = "Error while displaying result"
            resultTemplates[it.id] = "error"
          }
        }
      }
      return EvaluationViewModel(evaluation, resultContents, resultTemplates)
    }
  }
}
