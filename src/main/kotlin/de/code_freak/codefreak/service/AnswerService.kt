package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import de.code_freak.codefreak.util.afterClose
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils
import java.io.InputStream
import java.io.OutputStream
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
  private lateinit var taskService: TaskService

  fun getAnswerIdsForTaskIds(taskIds: Iterable<UUID>, userId: UUID) = answerRepository.findIdsForTaskIds(taskIds, userId).toMap()

  fun getAnswerIdForTaskId(taskId: UUID, userId: UUID): UUID = answerRepository.findIdForTaskId(taskId, userId)
      .orElseThrow { EntityNotFoundException("Answer not found.") }

  fun setFiles(answerId: UUID): OutputStream {
    return fileService.writeCollectionTar(answerId).afterClose { containerService.answerFilesUpdated(answerId) }
  }

  fun setFiles(answerId: UUID, files: InputStream) {
    setFiles(answerId).use { StreamUtils.copy(files, it) }
  }

  fun copyFilesFromTask(answer: Answer) {
    val taskDefinition = taskService.getTaskDefinition(answer.task.id)
    fileService.writeCollectionTar(answer.id).use { out ->
      fileService.readCollectionTar(answer.task.id).use { `in` ->
        val answerTar = TarArchiveOutputStream(out)
        val taskTar = TarArchiveInputStream(`in`)
        generateSequence { taskTar.nextTarEntry }
            .filter { !taskDefinition.isHidden(TarUtil.normalizeEntryName(it.name)) }
            .forEach {
              answerTar.putArchiveEntry(it)
              if (it.isFile) {
                StreamUtils.copy(taskTar, answerTar)
              }
              answerTar.closeArchiveEntry()
            }
      }
    }
  }

  fun TaskDefinition.isHidden(path: String): Boolean {
    val matcher = AntPathMatcher()
    hidden.plus("codefreak.yml").forEach {
      if (matcher.match(it, path)) return true
    }
    return false
  }
}
