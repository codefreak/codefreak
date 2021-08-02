package org.codefreak.codefreak.service

import java.util.UUID
import javax.annotation.PreDestroy
import org.codefreak.codefreak.Env
import org.codefreak.codefreak.repository.AnswerRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

@Service
@Profile(Env.DEV)
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "FILE_SYSTEM")
class CleanupDevFileSystemService {

  @Autowired
  lateinit var fileService: FileService

  @Autowired
  lateinit var taskRepository: TaskRepository

  @Autowired
  lateinit var answerRepository: AnswerRepository

  private val log = LoggerFactory.getLogger(this::class.java)

  @PreDestroy
  fun cleanupFileCollections() {
    val fileCollectionIds = mutableListOf<UUID>()

    taskRepository.findAll().forEach { fileCollectionIds.add(it.id) }
    answerRepository.findAll().forEach { fileCollectionIds.add(it.id) }

    fileCollectionIds.forEach {
      fileService.deleteCollection(it)
    }

    log.info("Cleaned up ${fileCollectionIds.size} file collections from disk")
  }
}
