package org.codefreak.codefreak.service

import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User

/**
 * This file contains all YAML data structures that are used for importing and exporting tasks/assignments.
 */
data class TaskDefinition(
  val title: String,
  val description: String? = null,
  val hidden: List<String> = emptyList(),
  val protected: List<String> = emptyList(),
  val evaluation: Map<String, EvaluationDefinition> = emptyMap(),
  val ide: IdeDefinition? = null
) {
  fun toEntity(assignment: Assignment?, owner: User, position: Long) = Task(
      assignment = assignment,
      owner = owner,
      position = position,
      title = title,
      body = description,
      weight = 100
  ).also { task ->
    task.hiddenFiles = hidden
    task.protectedFiles = protected
    task.evaluationStepDefinitions = evaluation.toList().mapIndexed { index, (key, definition) ->
      key to definition.toEntity(
          task,
          key,
          index
      )
    }.toMap().toMutableMap()
    task.ideEnabled = ide?.enabled ?: true
    task.ideArguments = ide?.cmd
    task.ideImage = ide?.image
  }
}

data class EvaluationDefinition(
  val title: String,
  val active: Boolean? = null,
  val script: String,
  val timeout: Long? = null,
  val report: EvaluationReportDefinition
) {
  fun toEntity(task: Task, key: String, position: Int) = EvaluationStepDefinition(
      key = key,
      task = task,
      position = position,
      title = title,
      script = script,
      report = report.toEntity()
  ).also {
    it.active = active ?: true
    it.timeout = timeout
  }
}

data class EvaluationReportDefinition(
  val format: String,
  val path: String
) {
  fun toEntity() = EvaluationStepDefinition.EvaluationStepReportDefinition(
      format = format,
      path = path
  )
}

data class AssignmentDefinition(
  val title: String,
  val tasks: List<String>
)

data class IdeDefinition(
  val enabled: Boolean,
  val image: String?,
  val cmd: String?
)

fun Task.toYamlDefinition() = TaskDefinition(
    title = title,
    description = body,
    hidden = hiddenFiles,
    protected = protectedFiles,
    evaluation = evaluationStepDefinitions.mapValues { (_, stepDefinition) -> stepDefinition.toYamlDefinition() },
    ide = IdeDefinition(
        enabled = ideEnabled,
        image = ideImage,
        cmd = ideArguments
    )
)

private fun EvaluationStepDefinition.EvaluationStepReportDefinition.toYamlDefinition() = EvaluationReportDefinition(
    format = format,
    path = path
)

private fun EvaluationStepDefinition.toYamlDefinition() = EvaluationDefinition(
    script = script,
    report = report.toYamlDefinition(),
    title = title,
    // only export active state if step is disabled
    active = if (!active) false else null
)
