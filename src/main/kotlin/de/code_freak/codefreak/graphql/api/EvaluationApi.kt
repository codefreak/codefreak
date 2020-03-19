package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import com.expediagroup.graphql.spring.operations.Subscription
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationStep
import de.code_freak.codefreak.entity.Feedback
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
import org.springframework.util.Base64Utils
import reactor.core.publisher.Flux
import java.util.UUID

@GraphQLName("PendingEvaluation")
class PendingEvaluationDto(answerEntity: Answer, ctx: ResolverContext) : BaseDto(ctx) {
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
class EvaluationStepDefinitionDto(val index: Int, definition: EvaluationDefinition) {
  val runnerName = definition.step
}

@GraphQLName("Evaluation")
class EvaluationDto(entity: Evaluation, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val answer by lazy { AnswerDto(entity.answer, ctx) }
  val createdAt = entity.createdAt
  val steps by lazy { entity.evaluationSteps.map { EvaluationStepDto(it) } }
  val stepsResultSummary by lazy {
    // use the worst result as global result
    steps.fold(EvaluationStepResultDto.SUCCESS) { acc, step ->
      if (step.result != null && step.result > acc) {
        step.result
      } else {
        acc
      }
    }
  }
}

@GraphQLName("EvaluationStep")
class EvaluationStepDto(entity: EvaluationStep) {
  val id = entity.id
  val runnerName = entity.runnerName
  val position = entity.position
  val result = entity.result?.let { EvaluationStepResultDto.valueOf(it.name) }
  val summary = entity.summary
  val feedback by lazy { entity.feedback.map { FeedbackDto(it) } }
}

@GraphQLName("EvaluationStepResult")
enum class EvaluationStepResultDto { SUCCESS, FAILED, ERRORED }

@GraphQLName("Feedback")
class FeedbackDto(entity: Feedback) {
  val id = entity.id
  val summary = entity.summary
  val fileContext = entity.fileContext?.let { FileContextDto(it) }
  val longDescription = entity.longDescription
  val group = entity.group
  val status = entity.status?.let { StatusDto.valueOf(it.name) }
  val severity = entity.severity?.let { SeverityDto.valueOf(it.name) }
}

@GraphQLName("FileContext")
class FileContextDto(entity: Feedback.FileContext) {
  val path = entity.path
  val lineStart = entity.lineStart
  val lineEnd = entity.lineEnd
  val columnStart = entity.columnStart
  val columnEnd = entity.columnEnd
}

@GraphQLName("FeedbackSeverity")
enum class SeverityDto {
  INFO,
  MINOR,
  MAJOR,
  CRITICAL
}

@GraphQLName("FeedbackStatus")
enum class StatusDto {
  IGNORE,
  SUCCESS,
  FAILED
}

@GraphQLName("PendingEvaluationUpdatedEventDto")
class PendingEvaluationUpdatedEventDto(event: PendingEvaluationUpdatedEvent) {
  val answerId = event.answerId
  val status = event.status
}

@Component
class EvaluationQuery : BaseResolver(), Query {

  @Secured(Authority.ROLE_STUDENT)
  fun evaluation(id: UUID): EvaluationDto = context {
    val evaluation = serviceAccess.getService(EvaluationService::class).getEvaluation(id)
    authorization.requireAuthorityIfNotCurrentUser(evaluation.answer.submission.user, Authority.ROLE_TEACHER)
    EvaluationDto(evaluation, this)
  }
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

  @Secured(Authority.ROLE_TEACHER)
  fun startAssignmentEvaluation(assignmentId: UUID): List<PendingEvaluationDto> = context {
    serviceAccess.getService(EvaluationService::class).startAssignmentEvaluation(assignmentId).map {
      PendingEvaluationDto(it, this)
    }
  }

  @Secured(Authority.ROLE_TEACHER)
  fun addCommentFeedback(
    answerId: UUID,
    digest: String,
    comment: String,
    severity: SeverityDto?,
    path: String?,
    line: Int?
  ): Boolean = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    val user = authorization.currentUser
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    val digestByteArray = Base64Utils.decodeFromString(digest)
    val feedback = evaluationService.createCommentFeedback(user, comment).apply {
      if (path != null) {
        fileContext = Feedback.FileContext(path).apply {
          lineStart = line
        }
      }
      this.severity = when {
        severity != null -> Feedback.Severity.valueOf(severity.name)
        else -> null
      }
      this.status = when {
        severity != null -> Feedback.Status.FAILED
        else -> null
      }
    }
    evaluationService.addCommentFeedback(answer, digestByteArray, feedback)
    true
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

  fun pendingEvaluationUpdated(answerId: UUID, env: DataFetchingEnvironment): Flux<PendingEvaluationUpdatedEventDto> =
      context(env) {
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
