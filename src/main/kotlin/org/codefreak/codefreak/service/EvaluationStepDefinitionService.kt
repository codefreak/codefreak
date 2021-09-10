package org.codefreak.codefreak.service

import java.util.UUID
import org.apache.commons.lang3.RandomStringUtils
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.util.PositionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationStepDefinitionService {

  @Autowired
  private lateinit var evaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  @Autowired
  private lateinit var taskService: TaskService

  fun createNewStepDefinition(task: Task): EvaluationStepDefinition {
    // add a random suffix to step keys to prevent collisions
    val randomKeySuffix = RandomStringUtils.randomAlphabetic(2).lowercase()
    return EvaluationStepDefinition(
        task = task,
        key = "step-$randomKeySuffix",
        position = task.evaluationStepDefinitions.size,
        script = """
          #!/usr/bin/env bash
          # This is a dummy script that will be executed using bash.
          # Please adjust it to your needs!
          #
          # You can also use other scripting languages if you start this script with a proper shebang, e.g.
          #  #!/usr/bin/env python3
          #  #!/usr/bin/env node
          #  #!/usr/bin/env php
          #
          echo 'No tests defined!';
          exit 1;
        """.trimIndent(),
        title = "Evaluation Step ${task.evaluationStepDefinitions.size + 1}",
        report = EvaluationStepDefinition.EvaluationStepReportDefinition(
            format = "default",
            path = ""
        )
    )
  }

  @Transactional
  fun setEvaluationStepDefinitionPosition(evaluationStepDefinition: EvaluationStepDefinition, newPosition: Long) {
    val task = evaluationStepDefinition.task
    PositionUtil.move(
        task.evaluationStepDefinitions,
        { it.key },
        evaluationStepDefinition.position.toLong(),
        newPosition,
        { position.toLong() },
        { position = it.toInt() })
    taskService.saveTask(task)
  }

  @Transactional
  fun updateEvaluationStepDefinition(evaluationStepDefinition: EvaluationStepDefinition): EvaluationStepDefinition {
    saveEvaluationStepDefinition(evaluationStepDefinition)
    taskService.invalidateLatestEvaluations(evaluationStepDefinition.task)
    return evaluationStepDefinition
  }

  fun findEvaluationStepDefinition(id: UUID): EvaluationStepDefinition = evaluationStepDefinitionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Evaluation step definition not found") }

  fun saveEvaluationStepDefinition(definition: EvaluationStepDefinition) = evaluationStepDefinitionRepository.save(
      definition
  )
}
