package org.codefreak.codefreak.service

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.cloud.WorkspaceIdentifier
import org.codefreak.codefreak.cloud.WorkspacePurpose
import org.codefreak.codefreak.cloud.WorkspaceService
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AnswerRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.FileUtil
import org.codefreak.codefreak.util.FrontendUtil
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.afterClose
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnswerService : BaseService() {

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var ideService: IdeService

  @Autowired
  private lateinit var submissionService: SubmissionService

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var workspaceService: WorkspaceService

  fun findAnswer(taskId: UUID, userId: UUID): Answer = answerRepository.findByTaskIdAndSubmissionUserId(taskId, userId)
      .orElseThrow { EntityNotFoundException("Answer not found.") }

  fun findAnswer(answerId: UUID): Answer = answerRepository.findById(answerId).orElseThrow { EntityNotFoundException("Answer not found.") }

  @Transactional
  fun findOrCreateAnswer(taskId: UUID, user: User): Answer {
    val assignmentId = taskService.findTask(taskId).assignment?.id
    return submissionService.findOrCreateSubmission(assignmentId, user)
        .let { it.getAnswer(taskId) ?: createAnswer(it, taskId) }
  }

  @Transactional
  fun deleteAnswer(answerId: UUID) {
    workspaceService.deleteWorkspace(
      WorkspaceIdentifier(purpose = WorkspacePurpose.ANSWER_IDE, reference = answerId.toString())
    )
    ideService.removeAnswerIdeContainers(answerId)
    answerRepository.deleteById(answerId)

    if (fileService.collectionExists(answerId)) {
      fileService.deleteCollection(answerId)
    }
  }

  @Transactional
  fun setFiles(answer: Answer): OutputStream {
    require(answer.isEditable) { "The answer is not editable anymore" }
    return fileService.writeCollectionTar(answer.id).afterClose {
      answer.updatedAt = Instant.now()
      ideService.answerFilesUpdatedExternally(answer.id)
    }
  }

  fun copyFilesFromTask(answer: Answer) {
    ideService.saveTaskFiles(answer.task)
    fileService.writeCollectionTar(answer.id).use { out ->
      fileService.readCollectionTar(answer.task.id).use { `in` ->
        TarUtil.copyEntries(TarArchiveInputStream(`in`), TarUtil.PosixTarArchiveOutputStream(out), filter = {
          !answer.task.isHidden(it)
        })
      }
    }
  }

  fun resetAnswerFiles(answer: Answer) {
    require(answer.isEditable) { "The answer is not editable anymore" }
    copyFilesFromTask(answer)
    ideService.answerFilesUpdatedExternally(answer.id)
  }

  fun copyFilesForEvaluation(answer: Answer): InputStream {
    val out = ByteArrayOutputStream()
    val outTar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(answer.id).use { answerFiles ->
      TarUtil.copyEntries(TarArchiveInputStream(answerFiles), outTar, filter = {
        !answer.task.isHidden(it) && !answer.task.isProtected(it)
      })
    }
    fileService.readCollectionTar(answer.task.id).use { taskFiles ->
      TarUtil.copyEntries(TarArchiveInputStream(taskFiles), outTar, filter = {
        answer.task.isHidden(it) || answer.task.isProtected(it)
      })
    }
    return ByteArrayInputStream(out.toByteArray())
  }

  private fun Task.isHidden(entry: TarArchiveEntry): Boolean {
    hiddenFiles.plus("codefreak.yml").forEach {
      if (FileUtil.matches(it, entry.name)) return true
    }
    return false
  }

  private fun Task.isProtected(entry: TarArchiveEntry): Boolean {
    protectedFiles.forEach {
      if (FileUtil.matches(it, entry.name)) return true
    }
    return false
  }

  @Transactional
  fun createAnswer(submission: Submission, taskId: UUID): Answer {
    val task = taskService.findTask(taskId)
    if (!task.isTesting() && task.assignment?.status != AssignmentStatus.OPEN) {
      throw IllegalStateException("Assignment is not open. Cannot create answer.")
    }
    val answer = answerRepository.save(Answer(submission, task))
    copyFilesFromTask(answer)
    return answer
  }

  private fun Task.isTesting() =
      this.owner == FrontendUtil.getCurrentUser() || FrontendUtil.getCurrentUser().hasAuthority(Authority.ROLE_ADMIN)
}
