package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.config.AppConfiguration
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.StreamUtils
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream
import java.io.OutputStream

abstract class BaseController {

  @Autowired
  protected lateinit var submissionService: SubmissionService

  @Autowired
  protected lateinit var assignmentService: AssignmentService

  @Autowired
  protected lateinit var answerService: AnswerService

  @Autowired
  protected lateinit var config: AppConfiguration

  protected val user: User
    get() = FrontendUtil.getCurrentUser()

  protected fun download(filename: String, input: InputStream): ResponseEntity<StreamingResponseBody> {
    return download(filename) { out ->
      StreamUtils.copy(input, out)
    }
  }

  protected fun download(filename: String, writer: (out: OutputStream) -> Unit): ResponseEntity<StreamingResponseBody> {
    // allow only alphanumeric characters for download name and replace everything else by dash
    val sanitizedFilename = filename.toLowerCase().replace("[^\\w_\\-.]+".toRegex(), "-").trim('-')
    val headers = HttpHeaders().apply {
      add("Content-Disposition", "attachment; filename=$sanitizedFilename")
    }
    return ResponseEntity(StreamingResponseBody(writer), headers, HttpStatus.OK)
  }
}
