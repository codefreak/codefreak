package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Controller
class EvaluationController : BaseController() {

  @Autowired
  lateinit var evaluationService: EvaluationService

  @Autowired
  lateinit var answerService: AnswerService

  @RestHandler
  @PostMapping("/evaluations")
  fun startEvaluation(@RequestParam("taskId") taskId: UUID) {
    val answerId = answerService.getAnswerIdForTaskId(taskId, user.id)
    evaluationService.queueEvaluation(answerId)
  }
}
