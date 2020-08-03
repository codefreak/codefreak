package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Mutation
import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.TaskService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class IdeMutation : BaseResolver(), Mutation {

  @Transactional
  fun startIde(type: String, id: UUID): String = context {
    when (type) {
      "answer" -> {
        val answer = serviceAccess.getService(AnswerService::class).findAnswer(id)

        val readOnly = !authorization.isCurrentUser(answer.submission.user)
        if (readOnly) {
          // only teachers are allowed to see foreign answers in read-only containers
          authorization.requireAuthority(Authority.ROLE_TEACHER)
        }

        serviceAccess.getService(ContainerService::class).startIdeContainer(answer, readOnly)
      }
      "task" -> {
        val task = serviceAccess.getService(TaskService::class).findTask(id)
        require(task.isEditable(authorization))
        serviceAccess.getService(ContainerService::class).startIdeContainer(task)
      }
      else -> throw IllegalArgumentException("Unknown type")
    }
  }
}
