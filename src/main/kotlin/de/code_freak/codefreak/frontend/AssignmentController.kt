package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.LatexService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.FrontendUtil
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID
import javax.servlet.http.HttpServletResponse

@Controller
class AssignmentController : BaseController() {
  @Autowired
  lateinit var assignmentService: AssignmentService

  @Autowired
  lateinit var containerService: ContainerService

  @Autowired
  lateinit var latexService: LatexService

  @Autowired
  lateinit var taskService: TaskService

  @Autowired
  lateinit var submissionService: SubmissionService

  @Autowired
  lateinit var fileService: FileService

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
    model.addAttribute("assignment", assignmentService.findAssignment(assignmentId))
    return "assignment"
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/ide")
  fun getAssignmentIde(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    model: Model
  ): String {
    val submission = getSubmission(assignmentId)
    // start a container based on the submission for the current task
    val answer = submission.getAnswerForTask(taskId)!!
    containerService.startIdeContainer(answer)
    val containerUrl = containerService.getIdeUrl(answer.id)

    model.addAttribute("ide_url", containerUrl)
    return "ide-redirect"
  }

  @PostMapping("/assignments/{assignmentId}/tasks/{taskId}/answers")
  fun createAnswer(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID
  ): String {
    val submission = getSubmission(assignmentId)
    containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    return "redirect:/assignments/$assignmentId"
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

  @GetMapping("/assignments/{assignmentId}/submission.pdf", produces = ["application/pdf"])
  @ResponseBody
  fun pdfExportSubmission(
    @PathVariable("assignmentId") assignmentId: UUID,
    response: HttpServletResponse
  ): StreamingResponseBody {
    val submission = getSubmission(assignmentId)
    val filename = submission.assignment.title.trim().replace("[^\\w]+".toRegex(), "-").toLowerCase()
    response.setHeader("Content-Disposition", "attachment; filename=$filename.pdf")
    return StreamingResponseBody { latexService.submissionToPdf(submission, it) }
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/answer.pdf", produces = ["application/pdf"])
  @ResponseBody
  fun pdfExportAnswer(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): StreamingResponseBody {
    val submission = getSubmission(assignmentId)
    val answer = submission.getAnswerForTask(taskId) ?: throw EntityNotFoundException("Answer not found")
    val filename = answer.task.title.trim().replace("[^\\w]+".toRegex(), "-").toLowerCase()
    response.setHeader("Content-Disposition", "attachment; filename=$filename.pdf")
    return StreamingResponseBody { latexService.answerToPdf(answer, it) }
  }

  /**
   * Returns the submission for the given assignment or creates one if there is none already.
   */
  private fun getSubmission(assignmentId: UUID): Submission {
    return submissionService.findSubmission(assignmentId, user.id).orElseGet {
      submissionService.createNewSubmission(assignmentService.findAssignment(assignmentId), user)
    }
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/source.tar", produces = ["application/tar"])
  @ResponseBody
  fun getSourceTar(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): StreamingResponseBody {
    val submission = getSubmission(assignmentId)
    val answer = containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    response.setHeader("Content-Disposition", "attachment; filename=source.tar")
    if (fileService.collectionExists(answer.id)) {
      return fileService.readCollectionTar(answer.id).use { FrontendUtil.streamResponse(it) }
    }
    return fileService.readCollectionTar(taskId).use { FrontendUtil.streamResponse(it) }
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/source.zip", produces = ["application/zip"])
  @ResponseBody
  fun getSourceZip(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): StreamingResponseBody {
    val submission = getSubmission(assignmentId)
    val answer = containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    response.setHeader("Content-Disposition", "attachment; filename=source.zip")
    val tar = fileService.readCollectionTar(if (fileService.collectionExists(answer.id)) answer.id else taskId)
    return tar.use { StreamingResponseBody { out -> TarUtil.tarToZip(it, out) } }
  }
}
