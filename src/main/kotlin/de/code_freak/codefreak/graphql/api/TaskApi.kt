package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Task")
class TaskDto(@GraphQLIgnore val entity: Task, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val position = entity.position.toInt()
  val body = entity.body
  val assignment by lazy { AssignmentDto(entity.assignment, serviceAccess) }

  fun answer(userId: UUID?): AnswerDto? {
    val answerService = serviceAccess.getService(AnswerService::class)

    val answer = if (userId == null || userId == FrontendUtil.getCurrentUser().id) {
      try {
        answerService.findAnswer(id, FrontendUtil.getCurrentUser().id)
      } catch (e: EntityNotFoundException) {
        null
      }
    } else {
      Authorization().requireAuthority(Authority.ROLE_TEACHER)
      answerService.findAnswer(id, userId)
    }

    return answer?.let { AnswerDto(it, serviceAccess) }
  }
}

@Component
class TaskQuery : Query {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun task(id: UUID): TaskDto {
    val taskService = serviceAccess.getService(TaskService::class)
    return TaskDto(taskService.findTask(id), serviceAccess)
  }
}
