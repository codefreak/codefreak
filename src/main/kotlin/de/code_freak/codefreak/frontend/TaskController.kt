package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.GitImportService
import de.code_freak.codefreak.service.ResourceLimitException
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.FrontendUtil
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.servlet.http.HttpServletResponse

@Controller
class TaskController : BaseController() {

  data class EvaluationStatus(val running: Boolean, val taskTitle: String, val url: String?)

  @Autowired(required = false)
  var gitImportService: GitImportService? = null

  @Autowired
  lateinit var taskService: TaskService

  @Autowired
  lateinit var containerService: ContainerService

  @Autowired
  lateinit var fileService: FileService

  @Autowired
  lateinit var evaluationService: EvaluationService

  @GetMapping("/tasks/{taskId}/ide")
  fun getOrStartIde(
    @PathVariable("taskId") taskId: UUID,
    redirectAttributes: RedirectAttributes,
    model: Model
  ): String {
    val submission = getOrCreateSubmissionForTask(taskId)
    // start a container based on the submission for the current task
    val answer = submission.getOrCreateAnswer(taskId)
    try {
      containerService.startIdeContainer(answer)
    } catch (e: ResourceLimitException) {
      redirectAttributes.addFlashAttribute("ideCouldNotBeStarted", true)
      return "redirect:" + urls.get(submission.assignment)
    }
    val containerUrl = containerService.getIdeUrl(answer.id)

    model.addAttribute("ide_url", containerUrl)
    return "ide-redirect"
  }

  @GetMapping("/tasks/{taskId}/source.tar", produces = ["application/tar"])
  @ResponseBody
  fun getSourceTar(
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): StreamingResponseBody {
    val submission = getOrCreateSubmissionForTask(taskId)
    val answer = containerService.saveAnswerFiles(submission.getOrCreateAnswer(taskId))
    response.setHeader("Content-Disposition", "attachment; filename=source.tar")
    if (fileService.collectionExists(answer.id)) {
      return fileService.readCollectionTar(answer.id).use { FrontendUtil.streamResponse(it) }
    }
    return fileService.readCollectionTar(taskId).use { FrontendUtil.streamResponse(it) }
  }

  @GetMapping("/tasks/{taskId}/source.zip", produces = ["application/zip"])
  @ResponseBody
  fun getSourceZip(
    @PathVariable("taskId") taskId: UUID,
    response: HttpServletResponse
  ): StreamingResponseBody {
    val submission = getOrCreateSubmissionForTask(taskId)
    val answer = containerService.saveAnswerFiles(submission.getOrCreateAnswer(taskId))
    response.setHeader("Content-Disposition", "attachment; filename=source.zip")
    val tar = fileService.readCollectionTar(if (fileService.collectionExists(answer.id)) answer.id else taskId)
    return tar.use { StreamingResponseBody { out -> TarUtil.tarToZip(it, out) } }
  }

  @PostMapping("/tasks/{taskId}/source")
  fun uploadSource(
    @PathVariable("taskId") taskId: UUID,
    @RequestParam("file") file: MultipartFile,
    model: RedirectAttributes
  ): String {
    val submission = getOrCreateSubmissionForTask(taskId)
    val answer = submission.getOrCreateAnswer(taskId)
    answerService.setFiles(answer).use { TarUtil.processUploadedArchive(file, it) }
    model.successMessage("Successfully uploaded source for task '${answer.task.title}'.")
    return "redirect:" + urls.get(submission.assignment)
  }

  @PostMapping("/tasks/{taskId}/git-import")
  fun gitImport(
    @PathVariable("taskId") taskId: UUID,
    @RequestParam remoteUrl: String,
    model: RedirectAttributes
  ): String {
    val submission = getOrCreateSubmissionForTask(taskId)
    val answer = submission.getOrCreateAnswer(taskId)
    try {
      gitImportService?.importFiles(remoteUrl, answer)
      model.addFlashAttribute(
          "successMessage",
          "Successfully imported source for task '${answer.task.title}' from $remoteUrl."
      )
    } catch (e: IllegalArgumentException) {
      model.addFlashAttribute("errorMessage", e.message)
    } catch (e: Exception) {
      model.addFlashAttribute(
          "errorMessage",
          "Could not import from $remoteUrl. Please check your Git URL."
      )
    }
    return "redirect:" + urls.get(submission.assignment)
  }

  /**
   * Returns the submission for the given task or creates one if there is none already.
   */
  fun getOrCreateSubmissionForTask(taskId: UUID): Submission {
    val assignmentId = taskService.findTask(taskId).assignment.id
    return super.getOrCreateSubmission(assignmentId)
  }

  @RestHandler
  @GetMapping("/tasks/{taskId}/evaluation")
  fun getEvaluationStatus(@PathVariable("taskId") taskId: UUID): EvaluationStatus {
    val submission = getOrCreateSubmissionForTask(taskId)
    val answer = submission.getOrCreateAnswer(taskId)
    val running = evaluationService.isEvaluationRunning(answer.id)
    val url = evaluationService.getLatestEvaluation(answer.id).map { urls.get(it) }.orElse(null)
    return EvaluationStatus(running, answer.task.title, url)
  }

  @Secured(Authority.ROLE_TEACHER)
  @PostMapping("/tasks")
  fun updateTask(
      @RequestParam("file") file: MultipartFile,
      @RequestParam(name = "taskId") taskId: UUID,
      model: RedirectAttributes
  ) = withErrorPage("/import") {

    ByteArrayOutputStream().use { out ->
      TarUtil.processUploadedArchive(file, out)
      val task = taskService.updateFromTar(out.toByteArray(), taskId)
      model.successMessage("Task '${task.title}' has been updated.")
      "redirect:" + urls.get(task.assignment)
    }
  }
}
