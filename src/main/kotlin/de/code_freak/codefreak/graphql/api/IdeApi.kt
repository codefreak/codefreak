package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Mutation
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.ContainerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class IdeMutation : Mutation {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Transactional
  fun startIde(type: String, id: UUID): String {
    if (type == "answer") {
      val answer = serviceAccess.getService(AnswerService::class).findAnswer(id)
      var readOnly = true
      if (Authorization().isCurrentUser(answer.submission.user)) {
        readOnly = answer.task.assignment.closed
      } else {
        Authorization().requireAuthority(Authority.ROLE_TEACHER)
      }
      val containerService = serviceAccess.getService(ContainerService::class)
      containerService.startIdeContainer(answer, readOnly)
      return containerService.getIdeUrl(answer.id, readOnly)
    }
    throw IllegalArgumentException("Unknown type")
  }
}
