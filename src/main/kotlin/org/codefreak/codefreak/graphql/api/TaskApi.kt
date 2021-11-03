package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.apache.catalina.core.ApplicationPart
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.GitImportService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.TaskTarService
import org.codefreak.codefreak.util.FrontendUtil
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.templates.TaskTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@GraphQLName("Task")
class TaskDto(@GraphQLIgnore val entity: Task, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val position = entity.position.toInt()
  val body = entity.body
  val createdAt = entity.createdAt
  val updatedAt = entity.updatedAt
  val defaultFiles = entity.defaultFiles
  val assignment by lazy { entity.assignment?.let { AssignmentDto(it, ctx) } }
  val inPool = entity.assignment == null
  val editable by lazy { entity.isEditable(authorization) }
  val exportUrl by lazy { FrontendUtil.getUriBuilder().path("/api/tasks/$id/export").build().toUriString() }
  val hiddenFiles by lazy {
    authorization.requireAuthorityIfNotCurrentUser(entity.owner, Authority.ROLE_ADMIN)
    entity.hiddenFiles.toTypedArray()
  }
  val protectedFiles by lazy {
    authorization.requireAuthorityIfNotCurrentUser(entity.owner, Authority.ROLE_ADMIN)
    entity.protectedFiles.toTypedArray()
  }
  val customWorkspaceImage by lazy {
    authorization.requireAuthorityIfNotCurrentUser(entity.owner, Authority.ROLE_ADMIN)
    entity.customWorkspaceImage
  }
  val runCommand by lazy {
    authorization.requireAuthorityIfNotCurrentUser(entity.owner, Authority.ROLE_ADMIN)
    entity.runCommand
  }

  val evaluationStepDefinitions by lazy {
    entity.evaluationStepDefinitions.map { (_, definition) -> EvaluationStepDefinitionDto(definition, ctx) }
  }

  fun answer(userId: UUID?): AnswerDto? {
    val answerService = serviceAccess.getService(AnswerService::class)

    val answer = if (userId == null || userId == authorization.currentUser.id) {
      try {
        answerService.findAnswer(id, FrontendUtil.getCurrentUser().id)
      } catch (e: EntityNotFoundException) {
        null
      }
    } else {
      authorization.requireAuthority(Authority.ROLE_TEACHER)
      answerService.findAnswer(id, userId)
    }

    return answer?.let { AnswerDto(it, ctx) }
  }
}

class TaskInput(var id: UUID, var title: String, var timeLimit: Long?) {
  constructor() : this(UUID.randomUUID(), "", null)
}

class TaskDetailsInput(var id: UUID = UUID.randomUUID()) {
  var body: String? = null
  var hiddenFiles: Array<String> = arrayOf()
  var protectedFiles: Array<String> = arrayOf()
  var runCommand: String? = null
  var defaultFiles: List<String>? = null
  var customWorkspaceImage: String? = null
}

@GraphQLName("TaskTemplate")
class TaskTemplateDto(taskTemplate: TaskTemplate) {
  val name: String = taskTemplate.name
  val title: String = taskTemplate.title
  val description: String = taskTemplate.description
}

@Component
class TaskQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_TEACHER)
  fun taskTemplates(): List<TaskTemplateDto> {
    return TaskTemplate.values().map { TaskTemplateDto(it) }
  }

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun task(id: UUID): TaskDto = context {
    val taskService = serviceAccess.getService(TaskService::class)
    val task = taskService.findTask(id)
    // only allow owner and admin to access tasks from the pool
    if (task.assignment == null) {
      authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    }
    TaskDto(task, this)
  }

  @Transactional
  @Secured(Authority.ROLE_TEACHER)
  fun taskPool() = context {
    serviceAccess.getService(TaskService::class)
        .getTaskPool(authorization.currentUser.id)
        .map { TaskDto(it, this) }
  }
}

@Component
class TaskMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_TEACHER)
  fun createTask(templateName: String?): TaskDto = context {
    val taskService = serviceAccess.getService(TaskTarService::class)
    val task = if (templateName != null) {
      taskService.createFromTemplateName(templateName, authorization.currentUser)
    } else {
      taskService.createEmptyTask(authorization.currentUser)
    }
    TaskDto(task, this)
  }

  @Secured(Authority.ROLE_TEACHER)
  fun deleteTask(id: UUID): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    serviceAccess.getService(TaskService::class).deleteTask(task)
    true
  }

  @Secured(Authority.ROLE_TEACHER)
  fun uploadTasks(files: Array<ApplicationPart>): List<TaskDto> = context {
    ByteArrayOutputStream().use {
      TarUtil.writeUploadAsTar(files, it)
      val tarContent = it.toByteArray()

      try {
        createSingleTask(tarContent, authorization.currentUser)
      } catch (e: IllegalArgumentException) {
        createMultipleTasks(tarContent, authorization.currentUser)
      }
    }
  }

  private fun createSingleTask(tarContent: ByteArray, owner: User): List<TaskDto> = context {
    val task = serviceAccess.getService(TaskTarService::class).createFromTar(tarContent, owner)
    listOf(TaskDto(task, this))
  }

  private fun createMultipleTasks(tarContent: ByteArray, owner: User): List<TaskDto> = context {
    val tasks = serviceAccess.getService(TaskTarService::class).createMultipleFromTar(tarContent, owner)
    tasks.map { task -> TaskDto(task, this) }
  }

  @Secured(Authority.ROLE_TEACHER)
  fun importTasks(url: String): List<TaskDto> = context {
    ByteArrayOutputStream().use {
      serviceAccess.getService(GitImportService::class).importFiles(url, it)
      val tarContent = it.toByteArray()

      try {
        createSingleTask(tarContent, authorization.currentUser)
      } catch (e: IllegalArgumentException) {
        createMultipleTasks(tarContent, authorization.currentUser)
      }
    }
  }

  @Secured(Authority.ROLE_TEACHER)
  fun updateTask(input: TaskInput): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(input.id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    task.title = input.title
    serviceAccess.getService(TaskService::class).saveTask(task)
    true
  }

  @Secured(Authority.ROLE_TEACHER)
  fun updateTaskDetails(input: TaskDetailsInput): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(input.id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    task.body = input.body
    task.hiddenFiles = input.hiddenFiles.filter { it.isNotBlank() }
    task.protectedFiles = input.protectedFiles.filter { it.isNotBlank() }
    task.runCommand = input.runCommand
    task.defaultFiles = input.defaultFiles?.filter { it.isNotBlank() }
    task.customWorkspaceImage = input.customWorkspaceImage?.trim()
    serviceAccess.getService(TaskService::class).saveTask(task)
    true
  }

  @Secured(Authority.ROLE_TEACHER)
  fun setTaskPosition(id: UUID, position: Long): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    serviceAccess.getService(TaskService::class).setTaskPosition(task, position)
    true
  }
}

fun Task.isEditable(authorization: Authorization) = when (assignment) {
  null -> authorization.isCurrentUser(owner) || authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
  else -> assignment?.isEditable(authorization) ?: false
}
