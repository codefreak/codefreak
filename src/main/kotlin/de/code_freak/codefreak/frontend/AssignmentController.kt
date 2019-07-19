package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.LatexService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
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

  @GetMapping("/admin/assignments/{assignmentId}/submissions.tar")
  @ResponseBody
  @Secured(Authority.ROLE_ADMIN)
  fun downloadSubmissionsArchive(@PathVariable("assignmentId") assignmentId: UUID, response: HttpServletResponse): ResponseEntity<ByteArray> {
    val assignment = assignmentService.findAssignment(assignmentId)
    return createTarDownload("${assignment.title}-submissions", submissionService.createTarArchiveOfSubmissions(assignmentId))
  }

  @GetMapping("/assignments/{assignmentId}/submission.pdf")
  @ResponseBody
  fun pdfExportSubmission(
    @PathVariable("assignmentId") assignmentId: UUID,
    response: HttpServletResponse
  ): ResponseEntity<ByteArray> {
    val submission = getSubmission(assignmentId)
    return createPdfDownload(submission.assignment.title, latexService.submissionToPdf(submission))
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/answer.pdf")
  @ResponseBody
  fun pdfExportAnswer(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID
  ): ResponseEntity<ByteArray> {
    val submission = getSubmission(assignmentId)
    val answer = submission.getAnswerForTask(taskId) ?: throw EntityNotFoundException("Answer not found")
    return createPdfDownload(answer.task.title, latexService.answerToPdf(answer))
  }

  /**
   * Returns the submission for the given assignment or creates one if there is none already.
   */
  private fun getSubmission(assignmentId: UUID): Submission {
    return submissionService.findSubmission(assignmentId, user.id).orElseGet {
      submissionService.createNewSubmission(assignmentService.findAssignment(assignmentId), user)
    }
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/source.tar")
  @ResponseBody
  fun getSourceTar(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): ResponseEntity<ByteArray> {
    val submission = getSubmission( assignmentId)
    val answer = containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    val binary = answer.files ?: taskService.findTask(taskId).files ?: throw EntityNotFoundException()
    return createTarDownload("source", binary)
  }

  @GetMapping("/assignments/{assignmentId}/tasks/{taskId}/source.zip")
  @ResponseBody
  fun getSourceZip(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): ResponseEntity<ByteArray> {
    val submission = getSubmission(assignmentId)
    val answer = containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    val tar = answer.files ?: taskService.findTask(taskId).files ?: throw EntityNotFoundException()
    return createZipDownload("source", TarUtil.tarToZip(tar))
  }

  private fun createDownload(mimeType: String, filename: String, binary: ByteArray): ResponseEntity<ByteArray> {
    val safeFilename = filename.trim().replace("[^\\w.]+".toRegex(), "-").toLowerCase()
    val headers = HttpHeaders()
    headers.add("Content-Type", mimeType)
    headers.add("Content-Disposition", "attachment; filename=$safeFilename")
    return ResponseEntity(binary, headers, HttpStatus.OK)
  }

  private fun createPdfDownload(filename: String, binary: ByteArray) = createDownload("application/pdf", "$filename.pdf", binary)
  private fun createTarDownload(filename: String, binary: ByteArray) = createDownload("application/tar", "$filename.tar", binary)
  private fun createZipDownload(filename: String, binary: ByteArray) = createDownload("application/zip", "$filename.zip", binary)
}
