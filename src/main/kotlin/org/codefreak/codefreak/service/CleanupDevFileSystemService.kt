package org.codefreak.codefreak.service

import java.util.UUID
import javax.annotation.PreDestroy
import javax.sql.DataSource
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

  @Autowired
  lateinit var dataSource: DataSource

  private val log = LoggerFactory.getLogger(this::class.java)

  @PreDestroy
  fun cleanupFileCollections() {
    if (!isInMemoryDatabase(dataSource)) {
      log.info("Not cleaning-up any file collections since no in-memory database is in use")
      return
    }

    val fileCollectionIds = mutableListOf<UUID>()

    taskRepository.findAll().forEach { fileCollectionIds.add(it.id) }
    answerRepository.findAll().forEach { fileCollectionIds.add(it.id) }

    fileCollectionIds.forEach {
      fileService.deleteCollection(it)
    }

    log.info("Cleaned up ${fileCollectionIds.size} file collections from disk")
  }

  private fun isInMemoryDatabase(dataSource: DataSource): Boolean {
    val url = dataSource.connection.metaData.url

    val isInMemoryH2Database = url.startsWith("jdbc:h2:mem")
    val isInMemoryHsqlDatabase = url.startsWith("jdbc:hsqldb:mem")
    val isInMemoryDerbyDatabase = url.startsWith("jdbc:derby:memory")
    val isInMemorySqliteDatabase = url.startsWith("jdbc:sqlite:memory")

    return isInMemoryH2Database || isInMemoryHsqlDatabase || isInMemoryDerbyDatabase || isInMemorySqliteDatabase
  }
}
