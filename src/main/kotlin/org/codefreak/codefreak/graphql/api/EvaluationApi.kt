package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import com.expediagroup.graphql.spring.operations.Subscription
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.auth.hasAuthority
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.graphql.BaseDto
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.graphql.ResolverContext
import org.codefreak.codefreak.graphql.SubscriptionEventPublisher
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.AssignmentService
import org.codefreak.codefreak.service.EvaluationStatusUpdatedEvent
import org.codefreak.codefreak.service.EvaluationStepDefinitionService
import org.codefreak.codefreak.service.EvaluationStepStatusUpdatedEvent
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.evaluation.EvaluationStepService
import org.codefreak.codefreak.service.evaluation.report.EvaluationReportFormatParser
import org.codefreak.codefreak.service.evaluation.report.FormatParserRegistry
import org.codefreak.codefreak.util.exhaustive
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@GraphQLName("EvaluationStepDefinition")
class EvaluationStepDefinitionDto(definition: EvaluationStepDefinition, ctx: ResolverContext) {
  val id = definition.id
  val key = definition.key
  val active = definition.active
  val position = definition.position
  val title = definition.title
  val timeout = definition.timeout
  val script by lazy {
    ctx.authorization.requireAuthorityIfNotCurrentUser(definition.task.owner, Authority.ROLE_ADMIN)
    definition.script
  }
  val reportFormat by lazy {
    ctx.authorization.requireAuthorityIfNotCurrentUser(definition.task.owner, Authority.ROLE_ADMIN)
    definition.report.format
  }
  val reportPath by lazy {
    ctx.authorization.requireAuthorityIfNotCurrentUser(definition.task.owner, Authority.ROLE_ADMIN)
    definition.report.path
  }
}

@GraphQLName("EvaluationStepDefinitionInput")
class EvaluationStepDefinitionInputDto(
  var id: UUID,
  var title: String,
  var active: Boolean,
  var timeout: Long?,
  var script: String,
  var reportFormat: String,
  var reportPath: String
)

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
  val stepsResultSummary by lazy { EvaluationStepResultDto(entity.stepsResultSummary) }
  val stepsStatusSummary by lazy { EvaluationStepStatusDto(entity.stepStatusSummary) }
}

@GraphQLName("EvaluationStep")
class EvaluationStepDto(entity: EvaluationStep, ctx: ResolverContext) {
  @GraphQLID
  val id = entity.id
  val definition by lazy { EvaluationStepDefinitionDto(entity.definition, ctx) }
  val result = entity.result?.let { EvaluationStepResultDto(it) }
  val summary = entity.summary
  val feedback by lazy { entity.feedback.map { FeedbackDto(it) } }
  val status = EvaluationStepStatusDto(entity.status)
  val queuedAt = entity.queuedAt
  val finishedAt = entity.finishedAt
}

@GraphQLName("EvaluationStepResult")
enum class EvaluationStepResultDto { SUCCESS, FAILED, ERRORED }

fun EvaluationStepResultDto(entity: EvaluationStepResult) = when (entity) {
  EvaluationStepResult.SUCCESS -> EvaluationStepResultDto.SUCCESS
  EvaluationStepResult.FAILED -> EvaluationStepResultDto.FAILED
  EvaluationStepResult.ERRORED -> EvaluationStepResultDto.ERRORED
}.exhaustive

@GraphQLName("EvaluationStepStatus")
enum class EvaluationStepStatusDto { PENDING, QUEUED, RUNNING, FINISHED, CANCELED }

fun EvaluationStepStatusDto(entity: EvaluationStepStatus) = when (entity) {
  EvaluationStepStatus.PENDING -> EvaluationStepStatusDto.PENDING
  EvaluationStepStatus.QUEUED -> EvaluationStepStatusDto.QUEUED
  EvaluationStepStatus.RUNNING -> EvaluationStepStatusDto.RUNNING
  EvaluationStepStatus.FINISHED -> EvaluationStepStatusDto.FINISHED
  EvaluationStepStatus.CANCELED -> EvaluationStepStatusDto.CANCELED
}.exhaustive

@GraphQLName("Feedback")
class FeedbackDto(entity: Feedback) {
  @GraphQLID
  val id = entity.id
  val summary = entity.summary
  val fileContext = entity.fileContext?.let { FileContextDto(it) }
  val longDescription = entity.longDescription
  val group = entity.group
  val status = entity.status?.let { StatusDto(it) }
  val severity = entity.severity?.let { SeverityDto(it) }
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

fun SeverityDto(entity: Feedback.Severity) = when (entity) {
  Feedback.Severity.INFO -> SeverityDto.INFO
  Feedback.Severity.MINOR -> SeverityDto.MINOR
  Feedback.Severity.MAJOR -> SeverityDto.MAJOR
  Feedback.Severity.CRITICAL -> SeverityDto.CRITICAL
}.exhaustive

@GraphQLName("FeedbackStatus")
enum class StatusDto {
  IGNORE,
  SUCCESS,
  FAILED
}

fun StatusDto(entity: Feedback.Status) = when (entity) {
  Feedback.Status.IGNORE -> StatusDto.IGNORE
  Feedback.Status.SUCCESS -> StatusDto.SUCCESS
  Feedback.Status.FAILED -> StatusDto.FAILED
}.exhaustive

@GraphQLName("EvaluationStatusUpdatedEvent")
class EvaluationStatusUpdatedEventDto(event: EvaluationStatusUpdatedEvent, ctx: ResolverContext) {
  val evaluation = EvaluationDto(event.evaluation, ctx)
  val status = EvaluationStepStatusDto(event.status)
}

@GraphQLName("EvaluationReportFormat")
class EvaluationReportFormatDto(parser: EvaluationReportFormatParser) {
  val key = parser.id
  val title = parser.title
}

@Component
class EvaluationQuery : BaseResolver(), Query {
  @Secured(Authority.ROLE_TEACHER)
  fun evaluationReportFormats(): List<EvaluationReportFormatDto> = context {
    serviceAccess.getService(FormatParserRegistry::class).allParsers.map { EvaluationReportFormatDto(it) }
  }

  @Secured(Authority.ROLE_STUDENT)
  fun evaluation(id: UUID): EvaluationDto = context {
    val evaluation = serviceAccess.getService(EvaluationService::class).getEvaluation(id)
    authorization.requireAuthorityIfNotCurrentUser(evaluation.answer.submission.user, Authority.ROLE_TEACHER)
    EvaluationDto(evaluation, this)
  }

  @Secured(Authority.ROLE_STUDENT)
  fun evaluationStep(stepId: UUID): EvaluationStepDto = context {
    val evaluationStep = serviceAccess.getService(EvaluationStepService::class).getEvaluationStep(stepId)
    authorization.requireAuthorityIfNotCurrentUser(evaluationStep.evaluation.answer.submission.user, Authority.ROLE_TEACHER)
    EvaluationStepDto(evaluationStep, this)
  }
}

@Component
class EvaluationMutation : BaseResolver(), Mutation {

  companion object {
    val objectMapper = ObjectMapper()
  }

  @Secured(Authority.ROLE_STUDENT)
  fun startEvaluation(answerId: UUID): EvaluationDto = context {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    val forceSaveFiles = authorization.isCurrentUser(answer.task.owner) || authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
    val evaluation = serviceAccess.getService(EvaluationService::class).startEvaluation(answer, forceSaveFiles)
    EvaluationDto(evaluation, this)
  }

  @Secured(Authority.ROLE_TEACHER)
  fun startAssignmentEvaluation(
    assignmentId: UUID,
    invalidateAll: Boolean?,
    invalidateTask: UUID?
  ): List<EvaluationDto> = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(assignmentId)
    authorization.requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    when {
      invalidateAll == true -> evaluationService.invalidateEvaluations(assignment)
      // find task in assignment and invalidate their evaluations + save modified task files from IDE
      // this also prevents passing IDs of foreign tasks
      invalidateTask != null -> assignment.tasks.find { it.id == invalidateTask }
          ?.let {
            evaluationService.invalidateEvaluations(it)
          }
    }

    evaluationService.startAssignmentEvaluation(assignmentId).map {
      EvaluationDto(it, this)
    }
  }

  fun createEvaluationStepDefinition(taskId: UUID) = context {
    val taskService = serviceAccess.getService(TaskService::class)
    val task = taskService.findTask(taskId)
    if (!task.isEditable(authorization)) {
      Authorization.deny()
    }
    val definitionService = serviceAccess.getService(EvaluationStepDefinitionService::class)
    task.addEvaluationStepDefinition(
        definitionService.createNewStepDefinition(task)
    )
    taskService.saveTask(task)
    taskService.invalidateLatestEvaluations(task)
    true
  }

  fun updateEvaluationStepDefinition(input: EvaluationStepDefinitionInputDto): Boolean = context {
    val definitionService = serviceAccess.getService(EvaluationStepDefinitionService::class)
    val definition = definitionService.findEvaluationStepDefinition(input.id)
    if (!definition.task.isEditable(authorization)) {
      Authorization.deny()
    }
    definition.title = input.title
    definition.script = input.script
    definition.active = input.active
    definition.timeout = input.timeout
    definition.report.path = input.reportPath
    definition.report.format = input.reportFormat
    definitionService.updateEvaluationStepDefinition(definition)
    true
  }

  fun deleteEvaluationStepDefinition(id: UUID) = context {
    val definitionService = serviceAccess.getService(EvaluationStepDefinitionService::class)
    val definition = definitionService.findEvaluationStepDefinition(id)
    if (!definition.task.isEditable(authorization)) {
      Authorization.deny()
    }
    val evaluationService = serviceAccess.getService(EvaluationService::class)
    check(definition.task.answers.none { evaluationService.isEvaluationScheduled(it.id) }) { "Cannot delete evaluation step while waiting for evaluation" }
    try {
      val task = definition.task
      task.deleteEvaluationStepDefinition(definition)
      serviceAccess.getService(TaskService::class).saveTask(task)
    } catch (e: DataIntegrityViolationException) {
      throw IllegalStateException("Evaluation steps cannot be deleted once used to generate feedback. You can deactivate it for future evaluation.")
    }
    // we do not need to invalidate evaluations here because we throw if there are any
    true
  }

  fun setEvaluationStepDefinitionPosition(id: UUID, position: Long): Boolean = context {
    val definitionService = serviceAccess.getService(EvaluationStepDefinitionService::class)
    val definition = definitionService.findEvaluationStepDefinition(id)
    if (!definition.task.isEditable(authorization)) {
      Authorization.deny()
    }
    definitionService.setEvaluationStepDefinitionPosition(definition, position)
    // we do not need to invalidate evaluations here because order does not matter
    true
  }
}

@Component
class EvaluationStatusUpdatedEventPublisher : SubscriptionEventPublisher<EvaluationStatusUpdatedEvent>()

@Component
class EvaluationStepStatusUpdatedEventPublisher : SubscriptionEventPublisher<EvaluationStepStatusUpdatedEvent>()

@Component
class EvaluationSubscription : BaseResolver(), Subscription {

  @Autowired
  private lateinit var evaluationStatusUpdatedEventPublisher: EvaluationStatusUpdatedEventPublisher

  @Autowired
  private lateinit var evaluationStepStatusUpdatedEventPublisher: EvaluationStepStatusUpdatedEventPublisher

  fun evaluationStatusUpdated(answerId: UUID?, status: EvaluationStepStatusDto?, env: DataFetchingEnvironment): Flux<EvaluationStatusUpdatedEventDto> =
      context(env) {
        val eventFilter = when {
          answerId != null -> buildAnswerFilter(env, answerId)
          else -> buildCurrentUserFilter(env)
        }
        evaluationStatusUpdatedEventPublisher.eventStream
            .filter(eventFilter)
            .filter(buildStatusFilter(status))
            .map { EvaluationStatusUpdatedEventDto(it, this) }
      }

  fun evaluationStepStatusUpdated(stepId: UUID, env: DataFetchingEnvironment): Flux<EvaluationStepDto> = context(env) {
    val evaluationStep = serviceAccess.getService(EvaluationStepService::class).getEvaluationStep(stepId)
    authorization.requireAuthorityIfNotCurrentUser(evaluationStep.evaluation.answer.submission.user, Authority.ROLE_TEACHER)
    evaluationStepStatusUpdatedEventPublisher.eventStream
        .filter { it.evaluationStep.id == stepId }
        .map { EvaluationStepDto(it.evaluationStep, this) }
  }

  private fun buildAnswerFilter(env: DataFetchingEnvironment, answerId: UUID): (event: EvaluationStatusUpdatedEvent) -> Boolean = context(env) {
    val answer = serviceAccess.getService(AnswerService::class).findAnswer(answerId)
    authorization.requireAuthorityIfNotCurrentUser(answer.submission.user, Authority.ROLE_TEACHER)
    return@context { it.evaluation.answer.id == answerId }
  }

  private fun buildCurrentUserFilter(env: DataFetchingEnvironment): (event: EvaluationStatusUpdatedEvent) -> Boolean = context(env) {
    val currentUser = authorization.currentUser
    return@context { it.evaluation.answer.submission.user == currentUser }
  }

  private fun buildStatusFilter(statusDto: EvaluationStepStatusDto?): (event: EvaluationStatusUpdatedEvent) -> Boolean {
    val status = statusDto?.let { EvaluationStepStatus.valueOf(statusDto.name) }
    return { status == null || it.status == status }
  }
}
