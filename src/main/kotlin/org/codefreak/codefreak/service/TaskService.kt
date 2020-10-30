package org.codefreak.codefreak.service

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import liquibase.util.StreamUtil
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.util.PositionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
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
  private lateinit var taskTarHelper: TaskTarHelper

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }

  @Transactional
  fun createFromTar(
    tarContent: ByteArray,
    owner: User,
    assignment: Assignment? = null,
    position: Long = 0L
  ): Task = taskTarHelper.createFromTar(tarContent, owner, assignment, position)

  @Transactional
  fun createMultipleFromTar(
    tarContent: ByteArray,
    owner: User,
    assignment: Assignment? = null,
    position: Long = 0L
  ) = taskTarHelper.createMultipleFromTar(tarContent, owner, assignment, position)

  @Transactional
  fun createEmptyTask(owner: User): Task {
    return ByteArrayOutputStream().use {
      StreamUtil.copy(ClassPathResource("empty_task.tar").inputStream, it)
      createFromTar(it.toByteArray(), owner)
    }
  }

  @Transactional
  fun deleteTask(task: Task) {
    task.assignment?.run {
      tasks.filter { it.position > task.position }.forEach { it.position-- }
      taskRepository.saveAll(tasks)
    }
    taskRepository.delete(task)
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

  @Transactional
  fun getExportTar(taskId: UUID) = getExportTar(findTask(taskId))

  @Transactional
  fun getExportTar(task: Task): ByteArray = taskTarHelper.getExportTar(task)

  @Transactional
  fun getExportTar(tasks: Collection<Task>): ByteArray = taskTarHelper.getExportTar(tasks)

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
