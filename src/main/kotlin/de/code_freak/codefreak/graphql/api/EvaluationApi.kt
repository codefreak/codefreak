package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLName
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationStep
import de.code_freak.codefreak.entity.Feedback
import de.code_freak.codefreak.graphql.ServiceAccess
import de.code_freak.codefreak.service.EvaluationDefinition

@GraphQLName("EvaluationStepDefinition")
class EvaluationStepDefinitionDto(val index: Int, definition: EvaluationDefinition) {
  val runnerName = definition.step
}

@GraphQLName("Evaluation")
class EvaluationDto(entity: Evaluation, serviceAccess: ServiceAccess) {
  @GraphQLID
  val id = entity.id
  val answer by lazy { AnswerDto(entity.answer, serviceAccess) }
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