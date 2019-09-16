package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.repository.TaskRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil.getYamlDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TaskService {

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var fileService: FileService

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }

  @Transactional(noRollbackFor = [Throwable::class])
  fun createFromTar(tarContent: ByteArray, assignment: Assignment, position: Long): Task {
    var task = getYamlDefinition<TaskDefinition>(tarContent.inputStream()).let {
      Task(assignment, position, it.title, it.description, 100)
    }
    task = taskRepository.save(task)
    fileService.writeCollectionTar(task.id).use { it.write(tarContent) }
    return task
  }

  fun getTaskDefinition(taskId: UUID) = fileService.readCollectionTar(taskId).use { getYamlDefinition<TaskDefinition>(it) }
}
