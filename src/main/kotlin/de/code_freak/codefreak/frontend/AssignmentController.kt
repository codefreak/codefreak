package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Role
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.GitImportService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.servlet.http.HttpServletResponse

@Controller
class AssignmentController : BaseController() {

  @Autowired(required = false)
  var gitImportService: GitImportService? = null

  @Autowired
  lateinit var evaluationService: EvaluationService

  @Autowired
  lateinit var containerService: ContainerService

  data class TaskInfo(
    val task: Task,
    val answerId: UUID?,
    val latestEvaluation: Evaluation?,
    val evaluationRunning: Boolean,
    val ideRunning: Boolean,
    val evaluationUpToDate: Boolean
  )

  @GetMapping("/assignments")
  fun getAssignment(model: Model): String {
    val assignments = if (user.authorities.contains(Role.TEACHER)) {
      assignmentService.findAllAssignments()
    } else {
      assignmentService.findAllAssignmentsForUser(user.entity.id)
    }
    model.addAttribute("assignments", assignments)
    return "assignments"
  }

  @GetMapping("/assignments/{id}")
  fun getAssignment(
    @PathVariable("id") assignmentId: UUID,
    model: Model
  ): String {
    val assignment = assignmentService.findAssignment(assignmentId)
    val answerIds = answerService.getAnswerIdsForTaskIds(assignment.tasks.map { it.id }, user.entity.id)
    val latestEvaluations = evaluationService.getLatestEvaluations(answerIds.values)
    val taskInfos = assignment.tasks.map {
      val answerId = answerIds[it.id]
      TaskInfo(
          task = it,
          answerId = answerId,
          evaluationRunning = if (answerId == null) false else evaluationService.isEvaluationRunning(answerId),
          latestEvaluation = if (answerId == null) null else latestEvaluations[answerId]?.orElse(null),
          evaluationUpToDate = answerId?.let { evaluationService.isEvaluationUpToDate(answerId) } ?: false,
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

  @Secured(Authority.ROLE_TEACHER)
  @PostMapping("/assignments")
  fun createAssignment(
    @RequestParam("file") file: MultipartFile,
    model: RedirectAttributes
  ) = withErrorPage("/import") {

    ByteArrayOutputStream().use { out ->
      TarUtil.processUploadedArchive(file, out)
      val result = assignmentService.createFromTar(out.toByteArray(), user.entity)
      model.successMessage("Assignment has been created")
      if (result.taskErrors.isNotEmpty()) {
        model.errorMessage("Not all tasks could be imported successfully:\n" + result.taskErrors.map { "${it.key}: ${it.value.message}" }.joinToString("\n"))
      }
      "redirect:" + urls.get(result.assignment)
    }
  }
}
