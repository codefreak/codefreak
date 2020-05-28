package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import org.apache.catalina.core.ApplicationPart
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.GitImportService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.util.FrontendUtil
import org.codefreak.codefreak.util.TarUtil
import org.springframework.core.io.ClassPathResource
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.util.UUID

@GraphQLName("Task")
class TaskDto(@GraphQLIgnore val entity: Task, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val position = entity.position.toInt()
  val body = entity.body
  val createdAt = entity.createdAt
  val assignment by lazy { entity.assignment?.let { AssignmentDto(it, ctx) } }
  val inPool = entity.assignment == null
  val editable by lazy { entity.isEditable(authorization) }
  val hiddenFiles by lazy {
    authorization.requireAuthorityIfNotCurrentUser(entity.owner, Authority.ROLE_ADMIN)
    entity.hiddenFiles.toTypedArray()
  }
  val protectedFiles by lazy {
    authorization.requireAuthorityIfNotCurrentUser(entity.owner, Authority.ROLE_ADMIN)
    entity.protectedFiles.toTypedArray()
  }

  val evaluationStepDefinitions by lazy {
    entity.evaluationStepDefinitions.sortedBy { it.position }.map { EvaluationStepDefinitionDto(it, ctx) }
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

enum class TaskTemplate {
  JAVA, PYTHON, CSHARP, JAVASCRIPT
}

class TaskInput(var id: UUID, var title: String) {
  constructor() : this(UUID.randomUUID(), "")
}

class TaskDetailsInput(var id: UUID, var body: String?, var hiddenFiles: Array<String>, var protectedFiles: Array<String>) {
  constructor() : this(UUID.randomUUID(), null, arrayOf(), arrayOf())
}

@Component
class TaskQuery : BaseResolver(), Query {

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun task(id: UUID): TaskDto = context {
    val taskService = serviceAccess.getService(TaskService::class)
    TaskDto(taskService.findTask(id), this)
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
  fun createTask(template: TaskTemplate?): TaskDto = context {
    if (template != null) {
      val templateTar = ClassPathResource("org/codefreak/templates/${template.name.toLowerCase()}.tar").inputStream.use { it.readBytes() }
      serviceAccess.getService(TaskService::class).createFromTar(templateTar, null, authorization.currentUser, 0)
          .let { TaskDto(it, this) }
    } else {
      serviceAccess.getService(TaskService::class).createEmptyTask(authorization.currentUser).let { TaskDto(it, this) }
    }
  }

  fun deleteTask(id: UUID): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    serviceAccess.getService(TaskService::class).deleteTask(task)
    true
  }

  @Secured(Authority.ROLE_TEACHER)
  fun uploadTask(files: Array<ApplicationPart>): TaskDto = context {
    ByteArrayOutputStream().use {
      TarUtil.writeUploadAsTar(files, it)
      val task = serviceAccess.getService(TaskService::class).createFromTar(it.toByteArray(), null, authorization.currentUser, 0L)
      TaskDto(task, this)
    }
  }

  @Secured(Authority.ROLE_TEACHER)
  fun importTask(url: String): TaskDto = context {
    ByteArrayOutputStream().use {
      serviceAccess.getService(GitImportService::class).importFiles(url, it)
      val task = serviceAccess.getService(TaskService::class).createFromTar(it.toByteArray(), null, authorization.currentUser, 0L)
      TaskDto(task, this)
    }
  }

  fun updateTask(input: TaskInput): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(input.id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    task.title = input.title
    serviceAccess.getService(TaskService::class).saveTask(task)
    true
  }

  fun updateTaskDetails(input: TaskDetailsInput): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(input.id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    task.body = input.body
    task.hiddenFiles = input.hiddenFiles.map { it.trim() }.filter { it.isNotEmpty() }
    task.protectedFiles = input.protectedFiles.map { it.trim() }.filter { it.isNotEmpty() }
    serviceAccess.getService(TaskService::class).saveTask(task)
    true
  }

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
