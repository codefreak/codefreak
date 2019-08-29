package de.code_freak.codefreak.service

import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.afterClose
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import sun.misc.IOUtils
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

  fun getAnswerIdsForTaskIds(taskIds: Iterable<UUID>, userId: UUID) = answerRepository.findIdsForTaskIds(taskIds, userId).toMap()

  fun getAnswerIdForTaskId(taskId: UUID, userId: UUID): UUID = answerRepository.findIdForTaskId(taskId, userId)
      .orElseThrow { EntityNotFoundException("Answer not found.") }

  fun setFiles(answerId: UUID): OutputStream {
    return fileService.writeCollectionTar(answerId).afterClose { containerService.answerFilesUpdated(answerId) }
  }

  fun setFiles(answerId: UUID, files: InputStream) {
    setFiles(answerId).use { StreamUtils.copy(files, it) }
  }
}
