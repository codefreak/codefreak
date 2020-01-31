package de.code_freak.codefreak.service.evaluation.runner

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Feedback
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.ExecResult
import de.code_freak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class CommandLineRunner : EvaluationRunner {

  data class Execution(val command: String, val result: ExecResult)

  @Autowired
  private lateinit var containerService: ContainerService

  override fun getName() = "commandline"

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    return executeCommands(answer, options, null).map { execution ->
      Feedback(execution.command).apply {
        longDescription = if (execution.result.output.isNotBlank()) {
          wrapInMarkdownCodeBlock(execution.result.output.trim())
        } else null
        status = if (execution.result.exitCode == 0L) Feedback.Status.SUCCESS else Feedback.Status.FAILED
      }
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
