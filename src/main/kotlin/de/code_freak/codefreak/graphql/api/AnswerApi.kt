package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.util.FrontendUtil
import de.code_freak.codefreak.util.TarUtil
import de.code_freak.codefreak.util.orNull
import org.apache.catalina.core.ApplicationPart
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Answer")
class AnswerDto(@GraphQLIgnore val entity: Answer, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val submission by lazy { SubmissionDto(entity.submission, serviceAccess) }
  val task by lazy { TaskDto(entity.task, serviceAccess) }
  val sourceUrl = FrontendUtil.getUriBuilder().path("/answers/$id/source").build().toUriString()

  val latestEvaluation by lazy {
    serviceAccess.getService(EvaluationService::class)
        .getLatestEvaluation(id)
        .map { EvaluationDto(it, serviceAccess) }
        .orNull()
  }
}

@Component
class AnswerMutation : Mutation {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Secured(Authority.ROLE_STUDENT)
  @Transactional
  fun uploadAnswerSource(id: UUID, files: Array<ApplicationPart>): Boolean {
    val answerService = serviceAccess.getService(AnswerService::class)
    val answer = answerService.findAnswer(id)
    Authorization.requireIsCurrentUser(answer.submission.user)
    answerService.setFiles(answer).use { TarUtil.writeUploadAsTar(files, it) }
    return true
  }
}

@Component
class AnswerQuery : Query {
  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun answer(id: UUID): AnswerDto {
    val answerService = serviceAccess.getService(AnswerService::class)
    val answer = answerService.findAnswer(id)
    if (Authorization.isCurrentUser(answer.submission.user)) {
      Authorization.requireAuthority(Authority.ROLE_TEACHER)
    }
    return AnswerDto(answer, serviceAccess)
  }
}
