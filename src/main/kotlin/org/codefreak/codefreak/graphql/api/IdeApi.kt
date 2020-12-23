package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.IdeService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.util.exhaustive
import org.springframework.stereotype.Component

enum class IdeType {
  ANSWER,
  TASK
}

@Component
class IdeMutation : BaseResolver(), Mutation {
  fun startIde(type: IdeType, id: UUID): String = context {
    when (type) {
      IdeType.ANSWER -> {
        val (answer, readOnly) = getAnswer(id)
        serviceAccess.getService(IdeService::class).startIdeContainer(answer, readOnly)
      }
      IdeType.TASK -> {
        val task = getTask(id)
        serviceAccess.getService(IdeService::class).startIdeContainer(task)
      }
    }.exhaustive
  }
}

@Component
class IdeQuery : BaseResolver(), Query {
  fun checkIdeLiveliness(type: IdeType, id: UUID): Boolean = context {
    when (type) {
      IdeType.ANSWER -> {
        val (answer, readOnly) = getAnswer(id)
        serviceAccess.getService(IdeService::class).checkIdeLiveliness(answer, readOnly)
      }
      IdeType.TASK -> {
        val task = getTask(id)
        serviceAccess.getService(IdeService::class).checkIdeLiveliness(task)
      }
    }.exhaustive
  }
}

private fun ResolverContext.getAnswer(id: UUID): Pair<Answer, Boolean> {
  val answer = serviceAccess.getService(AnswerService::class).findAnswer(id)

  val readOnly = !authorization.isCurrentUser(answer.submission.user)
  if (readOnly) {
    // only teachers are allowed to see foreign answers in read-only containers
    authorization.requireAuthority(Authority.ROLE_TEACHER)
  }
  return Pair(answer, readOnly)
}

private fun ResolverContext.getTask(id: UUID): Task {
  val task = serviceAccess.getService(TaskService::class).findTask(id)
  require(task.isEditable(authorization))
  return task
}
