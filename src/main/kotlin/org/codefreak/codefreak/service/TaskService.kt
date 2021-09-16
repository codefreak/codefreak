package org.codefreak.codefreak.service

import java.time.Instant
import java.util.UUID
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.PositionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class
TaskService : BaseService() {

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  private lateinit var fileService: FileService

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }

  @Transactional
  fun deleteTask(task: Task) {
    task.assignment?.run {
      tasks.filter { it.position > task.position }.forEach { it.position-- }
      taskRepository.saveAll(tasks)
    }
    taskRepository.delete(task)

    if (fileService.collectionExists(task.id)) {
      fileService.deleteCollection(task.id)
    }
  }

  @Transactional
  fun saveTask(task: Task) = taskRepository.save(task)

  @Transactional
  fun setTaskPosition(task: Task, newPosition: Long) {
    val assignment = task.assignment
    require(assignment != null) { "Task is not part of an assignment" }

    PositionUtil.move(assignment.tasks, task.position, newPosition, { position }, { position = it })

    taskRepository.saveAll(assignment.tasks)
    assignmentRepository.save(assignment)
  }

  fun getTaskPool(userId: UUID) = taskRepository.findByOwnerIdAndAssignmentIsNullOrderByCreatedAt(userId)

  /**
   * Makes sure that evaluations can be run on this task even if answer files
   * have not changed. Call this every time you update evaluation settings.
   */
  @Transactional
  fun invalidateLatestEvaluations(task: Task) {
    task.evaluationSettingsChangedAt = Instant.now()
    taskRepository.save(task)
  }
}
