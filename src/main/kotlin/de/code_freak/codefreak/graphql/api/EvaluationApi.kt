package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Subscription
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.authorized
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationResult
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.graphql.SubscriptionEventPublisher
import de.code_freak.codefreak.graphql.authorized
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.EvaluationFinishedEvent
import de.code_freak.codefreak.service.evaluation.EvaluationService
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.UUID

@GraphQLName("PendingEvaluation")
class PendingEvaluationDto(@GraphQLIgnore val answerEntity: Answer, @GraphQLIgnore val serviceAccess: ServiceAccess) {
  val answer by lazy { AnswerDto(answerEntity, serviceAccess) }
  val inQueue by lazy { serviceAccess.getService(EvaluationService::class).isEvaluationInQueue(answerEntity.id) }
}

@GraphQLName("Evaluation")
class EvaluationDto(@GraphQLIgnore val entity: Evaluation, @GraphQLIgnore val serviceAccess: ServiceAccess) {

  @GraphQLID
  val id = entity.id
  val answer by lazy { AnswerDto(entity.answer, serviceAccess) }
  val createdAt = entity.createdAt
  val results by lazy { entity.results.map { EvaluationResultDto(it) } }
}

@GraphQLName("EvaluationResult")
class EvaluationResultDto(@GraphQLIgnore val entity: EvaluationResult) {
  val runnerName = entity.runnerName
  val position = entity.position
  val error = entity.error
}

@Component
class EvaluationMutation : Mutation {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Secured(Authority.ROLE_STUDENT)
  fun startEvaluation(answerId: UUID): PendingEvaluationDto {
    return authorized {
      val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
      if (!isCurrentUser(answer.submission.user)) {
        requireAuthority(Authority.ROLE_TEACHER)
      }
      serviceAccess.getService(EvaluationService::class).startEvaluation(answer)
      PendingEvaluationDto(answer, serviceAccess)
    }
  }
}

@Component
class EvaluationFinishedEventPublisher : SubscriptionEventPublisher<EvaluationFinishedEvent>()

@Component
class EvaluationSubscription : Subscription {

  @Autowired
  private lateinit var serviceAccess: ServiceAccess

  @Autowired
  private lateinit var evaluationFinishedEventPublisher: EvaluationFinishedEventPublisher

  fun pendingEvaluationUpdated(): Flux<Int> = Flux.just(5)

  fun evaluationFinished(env: DataFetchingEnvironment): Flux<EvaluationDto> {
    return authorized(env, serviceAccess) {
      evaluationFinishedEventPublisher.eventStream
          .filter { it.evaluation.answer.submission.user == currentUser }
          .map { EvaluationDto(it.evaluation, serviceAccess) }
    }
  }
}
