package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.AppUser
import de.code_freak.codefreak.config.AppConfiguration
import de.code_freak.codefreak.entity.Submission
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.support.RequestContextUtils
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

abstract class BaseController {

  @Autowired
  protected lateinit var submissionService: SubmissionService

  @Autowired
  protected lateinit var assignmentService: AssignmentService

  @Autowired
  protected lateinit var answerService: AnswerService

  @Autowired
  protected lateinit var urls: Urls

  @Autowired
  protected lateinit var config: AppConfiguration

  protected val user: AppUser
    get() = FrontendUtil.getCurrentUser()

  /**
   * Returns the submission for the given assignment or creates one if there is none already.
   */
  protected fun getOrCreateSubmission(assignmentId: UUID): Submission {
    return submissionService.findSubmission(assignmentId, user.entity.id).orElseGet {
      submissionService.createSubmission(assignmentService.findAssignment(assignmentId), user.entity)
    }
  }

  fun Submission.getOrCreateAnswer(taskId: UUID) = getAnswer(taskId)
      ?: answerService.createAnswer(this, taskId)

  protected fun withErrorPage(path: String, block: () -> String): String {
    return try {
      block()
    } catch (e: Exception) {
      when (e) {
        is IllegalArgumentException, is IllegalStateException -> {
          RequestContextUtils.getOutputFlashMap(FrontendUtil.getRequest())["errorMessage"] = e.message
          "redirect:$path"
        } else -> throw e
      }
    }
  }

  protected fun RedirectAttributes.successMessage(message: String) = addFlashAttribute("successMessage", message)

  protected fun RedirectAttributes.errorMessage(message: String) = addFlashAttribute("errorMessage", message)

  protected fun parseLocalDateTime(str: String?, fieldName: String = "date"): Instant? {
    if (str.isNullOrEmpty()) return null
    try {
      val local = LocalDateTime.parse(str)
      return local.toInstant(config.l10n.timeZone.rules.getOffset(local))
    } catch (e: DateTimeParseException) {
      throw IllegalArgumentException("invalid $fieldName: ${e.message}")
    }
  }
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
