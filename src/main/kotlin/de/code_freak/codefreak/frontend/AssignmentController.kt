package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.GitImportService
import de.code_freak.codefreak.service.LatexService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID
import javax.servlet.http.HttpServletResponse

@Controller
class AssignmentController : BaseController() {

  @Autowired(required = false)
  var gitImportService: GitImportService? = null

  @Autowired
  lateinit var latexService: LatexService

  @Autowired
  lateinit var answerService: AnswerService

  @Autowired
  lateinit var evaluationService: EvaluationService

  @Autowired
  lateinit var containerService: ContainerService

  data class TaskInfo(val task: Task, val latestEvaluation: Evaluation?, val ideRunning: Boolean, val canStartEvaluation: Boolean)

  @GetMapping("/assignments")
  fun getAssignment(model: Model): String {
    model.addAttribute("assignments", assignmentService.findAllAssignments())
    return "assignments"
  }

  @GetMapping("/assignments/{id}")
  fun getAssignment(
    @PathVariable("id") assignmentId: UUID,
    model: Model
  ): String {
    val assignment = assignmentService.findAssignment(assignmentId)
    val answerIds = answerService.getAnswerIdsForTaskIds(assignment.tasks.map { it.id }, user.id)
    val latestEvaluations = evaluationService.getLatestEvaluations(answerIds.values)
    val taskInfos = assignment.tasks.map {
      val answerId = answerIds[it.id]
      TaskInfo(
          task = it,
          latestEvaluation = if (answerId == null) null else latestEvaluations[answerId]?.orElse(null),
          canStartEvaluation = answerId != null && !evaluationService.isEvaluationRunning(answerId),
          ideRunning = answerId != null && containerService.isIdeContainerRunning(answerId)
      ) }
    model.addAttribute("assignment", assignment)
    model.addAttribute("taskInfos", taskInfos)
    model.addAttribute("canStartNewIdeContainer", containerService.canStartNewIdeContainer())
    model.addAttribute("needsNewIdeContainer", taskInfos.any { taskInfo -> !taskInfo.ideRunning })
    model.addAttribute("supportedGitRemotes", gitImportService?.getSupportedHosts() ?: listOf<String>())
    return "assignment"
  }

  @GetMapping("/admin/assignments/{assignmentId}/submissions.tar", produces = ["application/tar"])
  @ResponseBody
  @Secured(Authority.ROLE_ADMIN)
  fun downloadSubmissionsArchive(@PathVariable("assignmentId") assignmentId: UUID, response: HttpServletResponse): StreamingResponseBody {
    val assignment = assignmentService.findAssignment(assignmentId)
    val filename = assignment.title.trim().replace("[^\\w]+".toRegex(), "-").toLowerCase()
    response.setHeader("Content-Disposition", "attachment; filename=$filename-submissions.tar")
    return StreamingResponseBody { submissionService.createTarArchiveOfSubmissions(assignmentId, it) }
  }
}
