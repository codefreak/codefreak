package org.codefreak.codefreak.service.evaluation.runner

import java.io.InputStream
import java.util.regex.Pattern
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.ExecResult
import org.codefreak.codefreak.util.withTrailingSlash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class CommandLineRunner : AbstractDockerRunner() {

  data class Execution(val command: String, val result: ExecResult)

  @Autowired
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var answerService: AnswerService

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

    return runInDocker(answer, image, projectPath, commands.toList(), stopOnFail, processFiles)
        .mapIndexed { index, result -> Execution(commands[index], result) }
  }

  protected fun runInDocker(
    answer: Answer,
    image: String,
    projectPath: String,
    commands: List<String>,
    stopOnFail: Boolean,
    processFiles: ((InputStream) -> Unit)? = null
  ): List<ExecResult> {
    val containerId = containerService.createContainer(image) {
      doNothingAndKeepAlive()
      containerConfig {
        workingDir(projectPath)
        env(buildEnvVariables(answer))
      }
      labels += getContainerLabelMap(answer)
    }
    val outputs = mutableListOf<ExecResult>()
    containerService.useContainer(containerId) {
      answerService.copyFilesForEvaluation(answer).use {
        containerService.copyToContainer(it, containerId, projectPath)
      }
      commands.forEach {
        if (stopOnFail && outputs.size > 0 && outputs.last().exitCode != 0L) {
          outputs.add(ExecResult("", -1))
        } else {
          outputs.add(containerService.exec(containerId, splitCommand(it)))
        }
      }
      if (processFiles !== null) {
        containerService.archiveContainer(containerId, "${projectPath.withTrailingSlash()}.", processFiles)
      }
      return outputs
    }
  }

  protected fun wrapInMarkdownCodeBlock(value: String) = "```\n$value\n```"

  private fun splitCommand(command: String): Array<String> {
    // from https://stackoverflow.com/a/366532/5519485
    val matchList = ArrayList<String>()
    val regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")
    val regexMatcher = regex.matcher(command)
    while (regexMatcher.find()) {
      when {
        regexMatcher.group(1) != null -> // Add double-quoted string without the quotes
          matchList.add(regexMatcher.group(1))
        regexMatcher.group(2) != null -> // Add single-quoted string without the quotes
          matchList.add(regexMatcher.group(2))
        else -> // Add unquoted word
          matchList.add(regexMatcher.group())
      }
    }
    return matchList.toArray(arrayOf())
  }
}
