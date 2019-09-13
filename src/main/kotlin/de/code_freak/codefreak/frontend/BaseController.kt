package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.AppUser
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

abstract class BaseController {

  @Autowired
  protected lateinit var submissionService: SubmissionService

  @Autowired
  protected lateinit var assignmentService: AssignmentService

  protected val user: AppUser
    get() = FrontendUtil.getCurrentUser()

  /**
   * Returns the submission for the given assignment or creates one if there is none already.
   */
  protected fun getOrCreateSubmission(assignmentId: UUID): Submission {
    return submissionService.findSubmission(assignmentId, user.entity.id).orElseGet {
      submissionService.createNewSubmission(assignmentService.findAssignment(assignmentId), user.entity)
    }
  }
}
