package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.ContainerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID
import javax.servlet.http.HttpServletRequest

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
    model: Model
  ): String {
    val submission = getSubmission(request, assignmentId)

    // start a container based on the submission for the current task
    val answer = submission.getAnswerForTask(taskId)!!
    containerService.startIdeContainer(answer)
    val containerUrl = containerService.getIdeUrl(answer.id)

    model.addAttribute("ide_url", containerUrl)
    return "ide-redirect"
  }

  @PostMapping("/assignments/{assignmentId}/tasks/{taskId}/answers")
  fun createAnswer(
    @PathVariable("assignmentId") assignmentId: UUID,
    @PathVariable("taskId") taskId: UUID,
    request: HttpServletRequest,
    model: Model
  ): String {
    val submission = getSubmission(request, assignmentId)
    containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    return "redirect:/assignments/$assignmentId"
  }

  private fun getSubmission(request: HttpServletRequest, assignmentId: UUID): Submission {
    // TODO: fetch submission by logged-in user and not from session
    val session = request.session
    val sessionKey = "assignment-$assignmentId-submission"
    var submissionId = session.getAttribute(sessionKey) as String?

    val submission = if (submissionId != null) {
        assignmentService.findSubmission(UUID.fromString(submissionId))
      } else {
        assignmentService.createNewSubmission(
            assignmentService.findAssignment(assignmentId)
        )
      }

    // store submission id for this task in session
    submissionId = submission.id.toString()
    session.setAttribute(sessionKey, submissionId)

    return submission
  }
}
