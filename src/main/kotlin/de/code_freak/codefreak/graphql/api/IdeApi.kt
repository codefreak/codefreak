package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Mutation
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.entity.AssignmentStatus
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.ContainerService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class IdeMutation : BaseResolver(), Mutation {

  @Transactional
  fun startIde(type: String, id: UUID): String = context {
    if (type == "answer") {
      val answer = serviceAccess.getService(AnswerService::class).findAnswer(id)
      var readOnly = true
      if (Authorization().isCurrentUser(answer.submission.user)) {
        readOnly = answer.task.assignment?.status != AssignmentStatus.OPEN
      } else {
        Authorization().requireAuthority(Authority.ROLE_TEACHER)
      }
      val containerService = serviceAccess.getService(ContainerService::class)
      containerService.startIdeContainer(answer, readOnly)
      containerService.getIdeUrl(answer.id, readOnly)
    } else {
      throw IllegalArgumentException("Unknown type")
    }
  }
}
