package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class AssignmentController : BaseController() {
  @Autowired
  lateinit var assignmentService: AssignmentService

  @Autowired
  lateinit var containerService: ContainerService

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
    request: HttpServletRequest,
    response: HttpServletResponse,
    model: Model
  ): String {
    // TODO: fetch submission by logged-in user and not from session
    val session = request.session
    val sessionKey = "assignment-$assignmentId-submission"
    var submissionId = session.getAttribute(sessionKey) as String?

    val submission: Submission
    try {
      submission = if (submissionId != null) {
        assignmentService.findSubmission(UUID.fromString(submissionId))
      } else {
        assignmentService.createNewSubmission(
            assignmentService.findAssignment(assignmentId)
        )
      }
    } catch (e: IllegalArgumentException) {
      // invalid UUID: delete submission and try again
      session.removeAttribute(sessionKey)
      return "redirect:/assignments/$assignmentId"
    } catch (e: EntityNotFoundException) {
      // submission has been deleted. We have to create a new one
      session.removeAttribute(sessionKey)
      return "redirect:/assignments/$assignmentId"
    }

    // store submission id for this task in session
    submissionId = submission.id.toString()
    session.setAttribute(sessionKey, submissionId)

    // start a container based on the submission for the current task
    val containerId = containerService.startIdeContainer(submission.getAnswerForTask(taskId)!!)
    val containerUrl = containerService.getIdeUrl(containerId)

    model.addAttribute("ide_url", containerUrl)
    return "ide-redirect"
  }
}
