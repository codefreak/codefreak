package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import com.expediagroup.graphql.spring.operations.Subscription
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetchingEnvironment
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.graphql.SubscriptionEventPublisher
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.EvaluationFinishedEvent
import org.codefreak.codefreak.service.PendingEvaluationUpdatedEvent
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.evaluation.EvaluationRunner
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.evaluation.PendingEvaluationStatus
import org.codefreak.codefreak.service.evaluation.isBuiltIn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import reactor.core.publisher.Flux
import java.lang.IllegalStateException
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
class EvaluationStepDefinitionDto(definition: EvaluationStepDefinition, ctx: ResolverContext) {
  companion object {
    private val objectMapper = ObjectMapper()
  }
  val id = definition.id
  val runnerName = definition.runnerName
  val active = definition.active
  val position = definition.position
  val title = definition.title
  val options: String by lazy {
    ctx.authorization.requireAuthorityIfNotCurrentUser(definition.task.owner, Authority.ROLE_ADMIN)
    objectMapper.writeValueAsString(definition.options)
  }
  val runner by lazy {
    ctx.authorization.requireAuthority(Authority.ROLE_TEACHER)
    ctx.serviceAccess.getService(EvaluationService::class).getEvaluationRunner(runnerName).let { EvaluationRunnerDto(it) }
  }
}

@GraphQLName("EvaluationStepDefinitionInput")
class EvaluationStepDefinitionInputDto(var id: UUID, var title: String, var active: Boolean, var options: String) {
  constructor() : this(UUID.randomUUID(), "", true, "")
}

@GraphQLName("Evaluation")
class EvaluationDto(entity: Evaluation, ctx: ResolverContext) : BaseDto(ctx) {
  @GraphQLID
  val id = entity.id
  val answer by lazy { AnswerDto(entity.answer, ctx) }
  val createdAt = entity.createdAt
  val steps by lazy {
    entity.evaluationSteps
        .filter { it.definition.active }
        .map { EvaluationStepDto(it, ctx) }
        .sortedBy { it.definition.position }
  }
  val stepsResultSummary by lazy { entity.stepsResultSummary.let { EvaluationStepResultDto.valueOf(it.name) } }
}

@GraphQLName("EvaluationStep")
class EvaluationStepDto(entity: EvaluationStep, ctx: ResolverContext) {
  @GraphQLID
  val id = entity.id
  val definition by lazy { EvaluationStepDefinitionDto(entity.definition, ctx) }
  val result = entity.result?.let { EvaluationStepResultDto.valueOf(it.name) }
  val summary = entity.summary
  val feedback by lazy { entity.feedback.map { FeedbackDto(it) } }
}
@GraphQLName("EvaluationRunner")
class EvaluationRunnerDto(runner: EvaluationRunner) {
  val name = runner.getName()
  val builtIn = runner.isBuiltIn()
  val defaultTitle = runner.getDefaultTitle()
  val optionsSchema = runner.getOptionsSchema()
}

@GraphQLName("EvaluationStepResult")
enum class EvaluationStepResultDto { SUCCESS, FAILED, ERRORED }

@GraphQLName("Feedback")
class FeedbackDto(entity: Feedback) {
  @GraphQLID
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

  @Secured(Authority.ROLE_TEACHER)
  fun evaluationRunners() = context {
    serviceAccess.getService(EvaluationService::class).getAllEvaluationRunners()
        .map { EvaluationRunnerDto(it) }
        .toTypedArray()
  }

  @Secured(Authority.ROLE_STUDENT)
  fun evaluation(id: UUID): EvaluationDto = context {
    val evaluation = serviceAccess.getService(EvaluationService::class).getEvaluation(id)
    authorization.requireAuthorityIfNotCurrentUser(evaluation.answer.submission.user, Authority.ROLE_TEACHER)
    EvaluationDto(evaluation, this)
  }
}

@Component
class EvaluationMutation : BaseResolver(), Mutation {

  companion object {
    val objectMapper = ObjectMapper()
  }

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

  fun createEvaluationStepDefinition(taskId: UUID, runnerName: String, options: String) = context {
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    val taskService = serviceAccess.getService(TaskService::class)
    val task = taskService.findTask(taskId)
    val runner = evaluationService.getEvaluationRunner(runnerName)
    if (!task.isEditable(authorization)) {
      Authorization.deny()
    }
    val optionsMap = objectMapper.readValue(options, object : TypeReference<HashMap<String, Any>>() {})
    val definition = EvaluationStepDefinition(task, runner.getName(), task.evaluationStepDefinitions.size, runner.getDefaultTitle(), optionsMap)
    evaluationService.validateRunnerOptions(definition)
    task.evaluationStepDefinitions.add(definition)
    evaluationService.saveEvaluationStepDefinition(definition)
    taskService.saveTask(task)
    true
  }

  fun updateEvaluationStepDefinition(input: EvaluationStepDefinitionInputDto): Boolean = context {
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    val definition = evaluationService.findEvaluationStepDefinition(input.id)
    if (!definition.task.isEditable(authorization)) {
      Authorization.deny()
    }
    definition.run {
      title = input.title
      active = input.active
      options = objectMapper.readValue(input.options, object : TypeReference<HashMap<String, Any>>() {})
    }
    evaluationService.validateRunnerOptions(definition)
    evaluationService.saveEvaluationStepDefinition(definition)
    true
  }

  fun deleteEvaluationStepDefinition(id: UUID) = context {
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    val definition = evaluationService.findEvaluationStepDefinition(id)
    if (!definition.task.isEditable(authorization)) {
      Authorization.deny()
    }
    require(!evaluationService.getEvaluationRunner(definition.runnerName).isBuiltIn()) { "Built-in evaluation steps cannot be deleted" }
    check(definition.task.answers.none { evaluationService.isEvaluationPending(it.id) }) { "Cannot delete evaluation step while evaluation is pending" }
    try {
      evaluationService.deleteEvaluationStepDefinition(definition)
    } catch (e: DataIntegrityViolationException) {
      throw IllegalStateException("Evaluation steps cannot be deleted once used to generate feedback. You can deactivate it for future evaluation.")
    }
    true
  }

  fun setEvaluationStepDefinitionPosition(id: UUID, position: Long): Boolean = context {
    val definition = serviceAccess.getService(EvaluationService::class).findEvaluationStepDefinition(id)
    if (!definition.task.isEditable(authorization)) {
      Authorization.deny()
    }
    serviceAccess.getService(EvaluationService::class).setEvaluationStepDefinitionPosition(definition, position)
    true
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
