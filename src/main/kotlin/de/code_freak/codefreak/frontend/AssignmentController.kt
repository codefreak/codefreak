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
import org.springframework.web.util.WebUtils
import java.util.UUID
import javax.servlet.http.Cookie
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
    val assignment = assignmentService.findAssignment(assignmentId)

    // TODO: fetch submission by logged-in user
    val cookieId = "assignment-$assignmentId-submission"
    val submission: Submission
    var cookie = WebUtils.getCookie(request, cookieId)
    try {
      submission = if (cookie != null) {
        assignmentService.findSubmission(UUID.fromString(cookie.value))
      } else {
        assignmentService.createNewSubmission(assignment)
      }
    } catch (e: IllegalArgumentException) {
      // invalid UUID: delete cookie and try again
      cookie!!.maxAge = 0
      response.addCookie(cookie)
      return "redirect:/assignments/$assignmentId"
    } catch (e: EntityNotFoundException) {
      // submission has been deleted. We have to create a new one
      cookie!!.maxAge = 0
      response.addCookie(cookie)
      return "redirect:/assignments/$assignmentId"
    }

    // send a cookie with the submission-id
    val submissionId = submission.id.toString()
    if (cookie == null) {
      cookie = Cookie(cookieId, submissionId)
      cookie.maxAge = 3600 * 60
    }
    response.addCookie(cookie)

    // start a container based on the submission for the current task
    val containerId = containerService.startIdeContainer(submission.forTask(taskId)!!)
    val containerUrl = containerService.getIdeUrl(containerId)

    model.addAttribute("ide_url", containerUrl)
    return "ide-redirect"
  }
}
