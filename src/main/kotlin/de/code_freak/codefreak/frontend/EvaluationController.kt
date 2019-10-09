package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Role
import de.code_freak.codefreak.service.evaluation.EvaluationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.UUID

@Controller
class EvaluationController : BaseController() {

  @Autowired
  lateinit var evaluationService: EvaluationService

  private val log = LoggerFactory.getLogger(this::class.java)

  @PostMapping("/evaluations")
  fun startEvaluation(@RequestParam("taskId") taskId: UUID, model: RedirectAttributes): String {
    val answer = answerService.getAnswerForTaskId(taskId, user.entity.id)
    val assignmentPage = urls.get(answer.task.assignment)
    return withErrorPage(assignmentPage) {
      answer.task.assignment.requireNotClosed()
      evaluationService.startEvaluation(answer)
      model.successMessage("Evaluation for task '${answer.task.title}' has been placed in the queue. " +
          "It may take some time depending on server load.")
      "redirect:$assignmentPage"
    }
  }

  @GetMapping("/evaluations/{evaluationId}")
  fun getEvaluation(@PathVariable("evaluationId") evaluationId: UUID, model: Model): String {
    val evaluation = evaluationService.getEvaluation(evaluationId)
    if (!user.authorities.contains(Role.TEACHER) && evaluation.answer.submission.user != user.entity) {
      throw AccessDeniedException("Cannot access evaluation")
    }
    val latestEvaluation = evaluationService.getLatestEvaluation(evaluation.answer.id).orElse(null)
    model.addAttribute("latestEvaluation", latestEvaluation)
    val isUpToDate = evaluation == latestEvaluation && evaluationService.isEvaluationUpToDate(evaluation.answer.id)
    model.addAttribute("isUpToDate", isUpToDate)

    val viewModel = EvaluationViewModel.create(evaluation, evaluationService)
    model.addAttribute("evaluation", evaluation)
    model.addAttribute("resultTemplates", viewModel.resultTemplates)
    model.addAttribute("resultContents", viewModel.resultContents)
    return "evaluation"
  }
}
