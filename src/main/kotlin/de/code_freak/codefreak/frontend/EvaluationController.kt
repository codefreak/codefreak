package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Controller
class EvaluationController : BaseController() {

  @Autowired
  lateinit var evaluationService: EvaluationService

  @Autowired
  lateinit var answerService: AnswerService

  private val log = LoggerFactory.getLogger(this::class.java)

  @RestHandler
  @PostMapping("/evaluations")
  fun startEvaluation(@RequestParam("taskId") taskId: UUID) {
    val answer = answerService.getAnswerForTaskId(taskId, user.id)
    evaluationService.startEvaluation(answer)
  }

  @GetMapping("/evaluations/{evaluationId}")
  fun getEvaluation(@PathVariable("evaluationId") evaluationId: UUID, model: Model): String {
    val evaluation = evaluationService.getEvaluation(evaluationId)
    // TODO authorization
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
    model.addAttribute("evaluation", evaluation)
    model.addAttribute("resultTemplates", resultTemplates)
    model.addAttribute("resultContents", resultContents)
    return "evaluation"
  }
}
