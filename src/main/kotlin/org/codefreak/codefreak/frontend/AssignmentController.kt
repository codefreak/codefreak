package org.codefreak.codefreak.frontend

import java.io.ByteArrayInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import kotlin.concurrent.thread
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.util.TarUtil
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Controller
@RequestMapping("/api/assignments")
class AssignmentController : BaseController() {

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/{id}/submissions.csv")
  fun getSubmissionsCsv(@PathVariable("id") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = getOwnedAssignment(assignmentId)
    val csv = submissionService.generateSubmissionCsv(assignmentId)
    return download("${assignment.title}-submissions.csv", csv.byteInputStream())
  }

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/{id}/submissions.tar")
  fun downloadSubmissionsTar(@PathVariable("id") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = getOwnedAssignment(assignmentId)
    return download("${assignment.title}-submissions.tar") {
      submissionService.generateSubmissionsTar(assignmentId, it)
    }
  }

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/{id}/submissions.zip")
  fun downloadSubmissionsZip(@PathVariable("id") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = getOwnedAssignment(assignmentId)
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

  private fun getOwnedAssignment(assignmentId: UUID): Assignment {
    val assignment = assignmentService.findAssignment(assignmentId)
    Authorization().requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    return assignment
  }

  @GetMapping("/{assignmentId}/export.tar", produces = ["application/tar"])
  @ResponseBody
  fun getExportTar(@PathVariable("assignmentId") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = assignmentService.findAssignment(assignmentId)
    Authorization().requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    val tar = assignmentService.getExportTar(assignment.id)
    return download("${assignment.title}.tar", ByteArrayInputStream(tar))
  }

  @GetMapping("/{assignmentId}/export.zip", produces = ["application/zip"])
  @ResponseBody
  fun getExportZip(@PathVariable("assignmentId") assignmentId: UUID): ResponseEntity<StreamingResponseBody> {
    val assignment = assignmentService.findAssignment(assignmentId)
    Authorization().requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    val zip = TarUtil.tarToZip(assignmentService.getExportTar(assignment.id))
    return download("${assignment.title}.zip", ByteArrayInputStream(zip))
  }
}
