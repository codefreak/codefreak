package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.util.TarUtil
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import kotlin.concurrent.thread

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

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/{id}/submissions.tar")
  fun downloadSubmissionsTar(@PathVariable("id") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = assignmentService.findAssignment(assignmentId)
    return download("${assignment.title}-submissions.tar") {
      submissionService.generateSubmissionsTar(assignmentId, it)
    }
  }

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/{id}/submissions.zip")
  fun downloadSubmissionsZip(@PathVariable("id") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = assignmentService.findAssignment(assignmentId)
    return download("${assignment.title}-submissions.zip") { downloadStream ->
      // pipe between tar writer and tar to zip conversion
      val outputPipe = PipedOutputStream()
      val inputPipe = PipedInputStream(outputPipe)
      thread {
        outputPipe.use {
          submissionService.generateSubmissionsTar(assignmentId, it)
        }
      }
      TarUtil.tarToZip(inputPipe, downloadStream)
    }
  }
}
