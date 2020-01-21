package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Subscription
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationStep
import de.code_freak.codefreak.graphql.BaseDto
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.graphql.ResolverContext
import de.code_freak.codefreak.graphql.SubscriptionEventPublisher
import de.code_freak.codefreak.service.AnswerService
import de.code_freak.codefreak.service.EvaluationDefinition
import de.code_freak.codefreak.service.EvaluationFinishedEvent
import de.code_freak.codefreak.service.PendingEvaluationUpdatedEvent
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.service.evaluation.PendingEvaluationStatus
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.UUID

@GraphQLName("PendingEvaluation")
class PendingEvaluationDto(@GraphQLIgnore val answerEntity: Answer, ctx: ResolverContext) : BaseDto(ctx) {
  val answer by lazy { AnswerDto(answerEntity, ctx) }
  val status by lazy {
    if (serviceAccess.getService(EvaluationService::class).isEvaluationInQueue(answerEntity.id)) {
      PendingEvaluationStatus.QUEUED
    } else {
      PendingEvaluationStatus.RUNNING
    }
  }
}

@GraphQLName("EvaluationStepDefinition")
class EvaluationStepDefinitionDto(val index: Int, @GraphQLIgnore val definition: EvaluationDefinition) {
  val runnerName = definition.step
}

@GraphQLName("Evaluation")
class EvaluationDto(@GraphQLIgnore val entity: Evaluation, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val answer by lazy { AnswerDto(entity.answer, ctx) }
  val createdAt = entity.createdAt
  val results by lazy { entity.evaluationSteps.map { EvaluationResultDto(it, ctx) } }
}

@GraphQLName("EvaluationResult")
class EvaluationResultDto(@GraphQLIgnore val entity: EvaluationStep, ctx: ResolverContext) {
  val runnerName = entity.runnerName
  val position = entity.position
  val error = entity.result == EvaluationStep.EvaluationStepResult.ERRORED
}

@GraphQLName("PendingEvaluationUpdatedEventDto")
class PendingEvaluationUpdatedEventDto(event: PendingEvaluationUpdatedEvent) {
  val answerId = event.answerId
  val status = event.status
}

@Component
class EvaluationMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_STUDENT)
  fun startEvaluation(answerId: UUID): PendingEvaluationDto = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    serviceAccess.getService(EvaluationService::class).startEvaluation(answer)
    PendingEvaluationDto(answer, this)
  }
}

@Component
class EvaluationFinishedEventPublisher : SubscriptionEventPublisher<EvaluationFinishedEvent>()

@Component
class PendingEvaluationUpdatedEventPublisher : SubscriptionEventPublisher<PendingEvaluationUpdatedEvent>()

@Component
class EvaluationSubscription : BaseResolver(), Subscription {

  @Autowired
  private lateinit var evaluationFinishedEventPublisher: EvaluationFinishedEventPublisher

  @Autowired
  private lateinit var pendingEvaluationUpdatedEventPublisher: PendingEvaluationUpdatedEventPublisher

  fun pendingEvaluationUpdated(answerId: UUID, env: DataFetchingEnvironment): Flux<PendingEvaluationUpdatedEventDto> = context(env) {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    pendingEvaluationUpdatedEventPublisher.eventStream
        .filter { it.answerId == answerId }
        .map { PendingEvaluationUpdatedEventDto(it) }
  }

  fun evaluationFinished(env: DataFetchingEnvironment): Flux<EvaluationDto> = context(env) {
    evaluationFinishedEventPublisher.eventStream
        .filter { it.evaluation.answer.submission.user == authorization.currentUser }
        .map { EvaluationDto(it.evaluation, this) }
  }
}
