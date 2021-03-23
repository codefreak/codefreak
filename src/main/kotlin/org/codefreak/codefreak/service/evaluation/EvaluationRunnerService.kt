package org.codefreak.codefreak.service.evaluation

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationRunnerService {

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var runners: List<EvaluationRunner>

  private val runnersByName by lazy { runners.map { it.getName() to it }.toMap() }

  fun getEvaluationRunner(name: String): EvaluationRunner = runnersByName[name]
      ?: throw IllegalArgumentException("Evaluation runner '$name' not found")

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getAllRunners() = runners

  /**
   * Returns the default options for the given evaluation runner
   */
  fun getDefaultOptions(runnerName: String) = getEvaluationRunner(runnerName).getDefaultOptions()

  fun isAutomated(runnerName: String): Boolean {
    return getEvaluationRunner(runnerName) is StoppableEvaluationRunner
  }

  fun validateRunnerOptions(definition: EvaluationStepDefinition) {
    val runner = getEvaluationRunner(definition.runnerName)
    val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6).getSchema(runner.getOptionsSchema())
    val errors = schema.validate(objectMapper.valueToTree(definition.options))
    require(errors.isEmpty()) { "Runner options for ${definition.runnerName} are invalid: \n" + errors.joinToString("\n") { it.message } }
  }

  @Transactional(readOnly = true)
  fun stopAnswerEvaluation(runnerName: String, answer: Answer) {
      val runner = getEvaluationRunner(runnerName)
      if (runner is StoppableEvaluationRunner) {
        runner.stop(answer)
      } else {
        log.warn("Cannot stop evaluation of runner ${runner.getName()}")
      }
  }
}
