package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.util.FrontendUtil
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
  val editable by lazy {
    when (entity.assignment) {
      null -> authorization.isCurrentUser(entity.owner) || authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
      else -> assignment?.editable ?: false
    }
  }

  val evaluationSteps by lazy {
    val taskDefinition = serviceAccess.getService(TaskService::class).getTaskDefinition(entity.id)
    taskDefinition.evaluation.mapIndexed { index, definition ->
      EvaluationStepDefinitionDto(index, definition)
    }
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

class TaskInput(var id: UUID, var title: String, var body: String?) {
  constructor() : this(UUID.randomUUID(), "", null)
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
  fun createTask(): TaskDto = context {
    serviceAccess.getService(TaskService::class).createEmptyTask(authorization.currentUser).let { TaskDto(it, this) }
  }

  fun deleteTask(id: UUID): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    serviceAccess.getService(TaskService::class).deleteTask(task.id)
    true
  }

  fun updateTask(input: TaskInput): Boolean = context {
    val task = serviceAccess.getService(TaskService::class).findTask(input.id)
    authorization.requireAuthorityIfNotCurrentUser(task.owner, Authority.ROLE_ADMIN)
    task.title = input.title
    task.body = input.body
    serviceAccess.getService(TaskService::class).saveTask(task)
    true
  }
}
