package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.AppUser
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.support.RequestContextUtils
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
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

  fun Submission.getOrCreateAnswer(taskId: UUID) = answers.firstOrNull { it.task.id == taskId }
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
}
