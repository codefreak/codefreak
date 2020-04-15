package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID

@Controller
@RequestMapping("/api/assignments")
class AssignmentController : BaseController() {

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/{id}/submissions.csv")
  fun getSubmissionsCsv(@PathVariable("id") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = assignmentService.findAssignment(assignmentId)
    val csv = submissionService.generateSubmissionCsv(assignmentId)
    return download("${assignment.title}-submissions.csv", csv.byteInputStream())
  }
}
