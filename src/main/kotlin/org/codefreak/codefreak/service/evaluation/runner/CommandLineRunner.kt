package org.codefreak.codefreak.service.evaluation.runner

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.ExecResult
import org.codefreak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class CommandLineRunner : EvaluationRunner {

  data class Execution(val command: String, val result: ExecResult)

  @Autowired
  private lateinit var containerService: ContainerService

  override fun getName() = "commandline"

  override fun getDefaultTitle() = "Command Line"

  override fun getDocumentationUrl() = "https://docs.codefreak.org/codefreak/for-teachers/definitions.html#commandline"

  override fun getOptionsSchema() = ClassPathResource("evaluation/commandline.schema.json").inputStream.use { String(it.readBytes()) }

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    return executeCommands(answer, options, null).map(this::executionToFeedback)
  }

  protected fun executionToFeedback(execution: Execution): Feedback {
    return Feedback(execution.command).apply {
      longDescription = if (execution.result.output.isNotBlank()) {
        wrapInMarkdownCodeBlock(execution.result.output.trim())
      } else null
      status = if (execution.result.exitCode == 0L) Feedback.Status.SUCCESS else Feedback.Status.FAILED
    }
  }

  override fun summarize(feedbackList: List<Feedback>): String {
    return if (feedbackList.any { feedback -> feedback.isFailed }) {
      "FAILED"
    } else {
      "OK"
    }
  }

  protected fun executeCommands(answer: Answer, options: Map<String, Any>, processFiles: ((InputStream) -> Unit)?): List<Execution> {
    val image = options.getRequired("image", String::class)
    val projectPath = options.getRequired("project-path", String::class)
    val commands = options.getList("commands", String::class, true)!!
    val stopOnFail = options.get("stop-on-fail", Boolean::class) ?: true

    return containerService.runCommandsForEvaluation(answer, image, projectPath, commands.toList(), stopOnFail, processFiles)
        .mapIndexed { index, result -> Execution(commands[index], result) }
  }

  protected fun wrapInMarkdownCodeBlock(value: String) = "```\n$value\n```"
}
