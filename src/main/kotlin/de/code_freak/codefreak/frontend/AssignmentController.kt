package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.service.LatexService
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

  @Autowired
  lateinit var latexService: LatexService

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
}
