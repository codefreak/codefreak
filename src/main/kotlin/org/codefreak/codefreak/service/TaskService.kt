package org.codefreak.codefreak.service

import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.service.evaluation.runner.CommentRunner
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil.getYamlDefinition
import liquibase.util.StreamUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.util.UUID

@Service
class
TaskService : BaseService() {

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var fileService: FileService

  @Transactional
  fun findTask(id: UUID): Task = taskRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Task not found") }

  @Transactional
  fun createFromTar(tarContent: ByteArray, assignment: Assignment?, owner: User, position: Long): Task {
    var task = getYamlDefinition<TaskDefinition>(tarContent.inputStream()).let {
      Task(assignment, owner, position, it.title, it.description, 100)
    }
    task = taskRepository.save(task)
    fileService.writeCollectionTar(task.id).use { it.write(tarContent) }
    return task
  }

  @Transactional
  fun createEmptyTask(owner: User): Task {
    return ByteArrayOutputStream().use {
      StreamUtil.copy(ClassPathResource("empty_task.tar").inputStream, it)
      createFromTar(it.toByteArray(), null, owner, 0)
    }
  }

  @Transactional
  fun deleteTask(id: UUID) = taskRepository.deleteById(id)

  @Transactional
  fun updateFromTar(tarContent: ByteArray, taskId: UUID): Task {
    var task = findTask(taskId)
    getYamlDefinition<TaskDefinition>(tarContent.inputStream()).let {
      task.title = it.title
      task.body = it.description
    }
    task = taskRepository.save(task)
    fileService.writeCollectionTar(task.id).use { it.write(tarContent) }
    return task
  }

  fun getTaskDefinition(taskId: UUID) = applyDefaultRunners(
      fileService.readCollectionTar(taskId).use { getYamlDefinition<TaskDefinition>(it) }
  )

  private fun applyDefaultRunners(taskDefinition: TaskDefinition): TaskDefinition {
    // add "comments" runner by default if not defined
    taskDefinition.run {
      if (evaluation.find { it.step == CommentRunner.RUNNER_NAME } == null) {
        return copy(evaluation = evaluation.toMutableList().apply {
          add(EvaluationDefinition(CommentRunner.RUNNER_NAME))
        })
      }
    }
    return taskDefinition
  }

  @Transactional
  fun saveTask(task: Task) = taskRepository.save(task)

  fun getTaskPool(userId: UUID) = taskRepository.findByOwnerIdAndAssignmentIsNullOrderByCreatedAt(userId)
}
