package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.LatexService
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.UUID
import javax.servlet.http.HttpServletResponse

@Controller
class TaskController : BaseController() {

  @Autowired
  lateinit var taskService: TaskService

  @Autowired
  lateinit var containerService: ContainerService

  @Autowired
  lateinit var latexService: LatexService

  @GetMapping("/tasks/{taskId}/ide")
  fun getAssignmentIde(
      @PathVariable("taskId") taskId: UUID,
      model: Model
  ): String {
    val submission = getSubmissionForTask(taskId)
    // start a container based on the submission for the current task
    val answer = submission.getAnswerForTask(taskId)!!
    containerService.startIdeContainer(answer)
    val containerUrl = containerService.getIdeUrl(answer.id)

    model.addAttribute("ide_url", containerUrl)
    return "ide-redirect"
  }

  @PostMapping("/tasks/{taskId}/answers")
  fun createAnswer(
      @PathVariable("taskId") taskId: UUID
  ): String {
    val submission = getSubmissionForTask(taskId)
    containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    val assignmentId = taskService.findTask(taskId).id
    return "redirect:/assignments/$assignmentId"
  }

  @GetMapping("/tasks/{taskId}/source.tar", produces = ["application/tar"])
  @ResponseBody
  fun getSourceTar(
      @PathVariable("taskId") taskId: UUID,
      response: HttpServletResponse
  ): ByteArray {
    val submission = getSubmissionForTask(taskId)
    val answer = containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    response.setHeader("Content-Disposition", "attachment; filename=source.tar")
    return answer.files ?: taskService.findTask(taskId).files ?: throw EntityNotFoundException()
  }

  @GetMapping("/tasks/{taskId}/source.zip", produces = ["application/zip"])
  @ResponseBody
  fun getSourceZip(
      @PathVariable("taskId") taskId: UUID,
      response: HttpServletResponse
  ): ByteArray {
    val submission = getSubmissionForTask(taskId)
    val answer = containerService.saveAnswerFiles(submission.getAnswerForTask(taskId)!!)
    response.setHeader("Content-Disposition", "attachment; filename=source.zip")
    val tar = answer.files ?: taskService.findTask(taskId).files ?: throw EntityNotFoundException()
    return TarUtil.tarToZip(tar)
  }

  @GetMapping("/tasks/{taskId}/answer.pdf", produces = ["application/pdf"])
  @ResponseBody
  fun pdfExportAnswer(
      @PathVariable("taskId") taskId: UUID,
      response: HttpServletResponse
  ): ByteArray {
    val submission = getSubmissionForTask(taskId)
    val answer = submission.getAnswerForTask(taskId) ?: throw EntityNotFoundException("Answer not found")
    val filename = answer.task.title.trim().replace("[^\\w]+".toRegex(), "-").toLowerCase()
    response.setHeader("Content-Disposition", "attachment; filename=$filename.pdf")
    return latexService.answerToPdf(answer)
  }

  /**
   * Returns the submission for the given task or creates one if there is none already.
   */
  fun getSubmissionForTask(taskId: UUID): Submission {
    val assignmentId = taskService.findTask(taskId).assignment.id
    return super.getSubmission(assignmentId)
  }
}
