package org.codefreak.codefreak.service

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AnswerRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.afterClose
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.AntPathMatcher
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.util.UUID

@Service
class AnswerService : BaseService() {

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var submissionService: SubmissionService

  @Autowired
  private lateinit var taskService: TaskService

  fun findAnswer(taskId: UUID, userId: UUID): Answer = answerRepository.findByTaskIdAndSubmissionUserId(taskId, userId)
      .orElseThrow { EntityNotFoundException("Answer not found.") }

  fun findAnswer(answerId: UUID): Answer = answerRepository.findById(answerId).orElseThrow { EntityNotFoundException("Answer not found.") }

  @Transactional
  fun findOrCreateAnswer(taskId: UUID, user: User): Answer {
    val assignmentId = taskService.findTask(taskId).assignment?.id
            ?: throw IllegalArgumentException("Task is not part of an assignment")
    return submissionService.findOrCreateSubmission(assignmentId, user)
        .let { it.getAnswer(taskId) ?: createAnswer(it, taskId) }
  }

  fun setFiles(answer: Answer): OutputStream {
    answer.task.assignment?.requireOpen()
    return fileService.writeCollectionTar(answer.id).afterClose { containerService.answerFilesUpdated(answer.id) }
  }

  fun copyFilesFromTask(answer: Answer) {
    containerService.saveTaskFiles(answer.task)
    val taskDefinition = taskService.getTaskDefinition(answer.task.id)
    fileService.writeCollectionTar(answer.id).use { out ->
      fileService.readCollectionTar(answer.task.id).use { `in` ->
        TarUtil.copyEntries(TarArchiveInputStream(`in`), TarUtil.PosixTarArchiveOutputStream(out)) {
          !taskDefinition.isHidden(it)
        }
      }
    }
  }

  fun copyFilesForEvaluation(answer: Answer): InputStream {
    val taskDefinition = taskService.getTaskDefinition(answer.task.id)
    val out = ByteArrayOutputStream()
    val outTar = TarUtil.PosixTarArchiveOutputStream(out)
    fileService.readCollectionTar(answer.id).use { answerFiles ->
      TarUtil.copyEntries(TarArchiveInputStream(answerFiles), outTar) {
        !taskDefinition.isHidden(it) && !taskDefinition.isProtected(it)
      }
    }
    fileService.readCollectionTar(answer.task.id).use { taskFiles ->
      TarUtil.copyEntries(TarArchiveInputStream(taskFiles), outTar) {
        taskDefinition.isHidden(it) || taskDefinition.isProtected(it)
      }
    }
    return ByteArrayInputStream(out.toByteArray())
  }

  private fun TaskDefinition.isHidden(entry: TarArchiveEntry): Boolean {
    val path = TarUtil.normalizeEntryName(entry.name)
    val matcher = AntPathMatcher()
    hidden.plus("codefreak.yml").forEach {
      if (matcher.match(it, path)) return true
    }
    return false
  }

  private fun TaskDefinition.isProtected(entry: TarArchiveEntry): Boolean {
    val path = TarUtil.normalizeEntryName(entry.name)
    val matcher = AntPathMatcher()
    protected.forEach {
      if (matcher.match(it, path)) return true
    }
    return false
  }

  @Transactional
  fun createAnswer(submission: Submission, taskId: UUID): Answer {
    val answer = answerRepository.save(Answer(submission, taskService.findTask(taskId)))
    copyFilesFromTask(answer)
    return answer
  }
}
