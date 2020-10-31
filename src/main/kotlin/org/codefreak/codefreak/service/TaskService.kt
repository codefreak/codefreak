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

  /**
   * Creates and saves a new task from the given tar.
   * If the task already exists in the repository and the tar content is newer,
   * no new task is created and the existing task is updated instead.
   *
   * @param tarContent the tar containing a task
   * @param owner the owner of the task
   * @param assignment the assignment the task belongs to, if any
   * @param position the position of the task if it belongs to an assignment
   * @return the created or updated task
   */
  @Transactional
  fun createFromTar(
    tarContent: ByteArray,
    owner: User,
    assignment: Assignment? = null,
    position: Long = 0L
  ): Task = taskTarHelper.createFromTar(tarContent, owner, assignment, position)

  /**
   * Creates and saves multiple tasks from the given tar.
   * The tar has to contain the individual tasks as tar archives themselves.
   * Tasks that already exist, but are newer in the archive are updated,
   * new tasks are created and saved to the repository.
   *
   * @param tarContent the tar containing multiple tasks as tar archives
   * @param owner the owner of the tasks
   * @param assignment the assignment the tasks belong to, if any
   * @return the created or updated tasks.
   */
  fun createMultipleFromTar(
    tarContent: ByteArray,
    owner: User,
    assignment: Assignment? = null
  ): List<Task> = taskTarHelper.createMultipleFromTar(tarContent, owner, assignment)

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

  /**
   * Creates a tar archive of the task with the given id.
   *
   * @param taskId the id of the task to be exported
   * @return a tar archive containing the task
   */
  @Transactional
  fun getExportTar(taskId: UUID) = getExportTar(findTask(taskId))

  /**
   * Creates a tar archive of the given task.
   *
   * @param task the task to be exported
   * @return a tar archive containing the task
   */
  @Transactional
  fun getExportTar(task: Task): ByteArray = taskTarHelper.getExportTar(task)

  /**
   * Creates a tar archive containing the given tasks each as a tar archive.
   *
   * @param tasks the tasks to be exported
   * @return a tar containing the exported tasks
   */
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
