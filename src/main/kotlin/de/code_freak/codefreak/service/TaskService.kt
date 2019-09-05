package de.code_freak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.repository.TaskRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.util.UUID
import javax.transaction.Transactional

@Service
class TaskService {

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var fileService: FileService

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }

  @Transactional
  fun createFromTar(tarContent: ByteArray, assignment: Assignment, position: Long): Task {
    var task = getTaskDefinition(tarContent.inputStream()).let {
      Task(assignment, position, it.title, it.description, 100)
    }
    task = taskRepository.save(task)
    fileService.writeCollectionTar(task.id).use { it.write(tarContent) }
    return task
  }

  private fun getTaskDefinition(`in`: InputStream): TaskDefinition {
    TarArchiveInputStream(`in`).let { tar -> generateSequence { tar.nextTarEntry }.forEach {
      if (it.isFile && TarUtil.normalizeEntryName(it.name) == "codefreak.yml") {
        val mapper = ObjectMapper(YAMLFactory())
        return mapper.readValue(tar, TaskDefinition::class.java)
      }
    } }
    throw IllegalArgumentException("Task does not contain codefreak.yml")
  }
}
