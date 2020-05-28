package org.codefreak.codefreak.frontend

import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.ByteArrayInputStream
import java.util.UUID

@Controller
@RequestMapping("/api/tasks")
class TaskController : BaseController() {

  @Autowired
  lateinit var taskService: TaskService

  @Autowired
  lateinit var containerService: ContainerService

  @Autowired
  lateinit var fileService: FileService

  @GetMapping("/{taskId}/source.tar", produces = ["application/tar"])
  @ResponseBody
  fun getSourceTar(@PathVariable("taskId") taskId: UUID): ResponseEntity<StreamingResponseBody> {
    val task = taskService.findTask(taskId)
    val submission = submissionService.findSubmission(task.assignment?.id ?: throw IllegalArgumentException(), user.id).get()
    val answer = containerService.saveAnswerFiles(submission.getAnswer(taskId) ?: throw IllegalArgumentException())
    fileService.readCollectionTar(if (fileService.collectionExists(answer.id)) answer.id else taskId).use {
      return download("${answer.task.title}.tar", it)
    }
  }

  @GetMapping("/{taskId}/source.zip", produces = ["application/zip"])
  @ResponseBody
  fun getSourceZip(@PathVariable("taskId") taskId: UUID): ResponseEntity<StreamingResponseBody> {
    val task = taskService.findTask(taskId)
    val submission = submissionService.findSubmission(task.assignment?.id ?: throw IllegalArgumentException(), user.id).get()
    val answer = containerService.saveAnswerFiles(submission.getAnswer(taskId) ?: throw IllegalArgumentException())
    fileService.readCollectionTar(if (fileService.collectionExists(answer.id)) answer.id else taskId).use {
      return download("${answer.task.title}.zip") {
        out -> TarUtil.tarToZip(it, out)
      }
    }
  }

  @GetMapping("/{taskId}/export.tar", produces = ["application/tar"])
  @ResponseBody
  fun getExportTar(@PathVariable("taskId") taskId: UUID): ResponseEntity<StreamingResponseBody> {
    val task = taskService.findTask(taskId)
    Authorization().requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    val tar = taskService.getExportTar(task.id)
    return download("${task.title}.tar", ByteArrayInputStream(tar))
  }

  @GetMapping("/{taskId}/export.zip", produces = ["application/zip"])
  @ResponseBody
  fun getExportZip(@PathVariable("taskId") taskId: UUID): ResponseEntity<StreamingResponseBody> {
    val task = taskService.findTask(taskId)
    Authorization().requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    val tar = taskService.getExportTar(task.id)
    return download("${task.title}.zip") {
      TarUtil.archiveToTar(ByteArrayInputStream(tar), it)
    }
  }
}
