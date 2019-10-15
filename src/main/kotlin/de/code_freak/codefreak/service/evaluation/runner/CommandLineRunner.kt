package de.code_freak.codefreak.service.evaluation.runner

import com.fasterxml.jackson.databind.ObjectMapper
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.ExecResult
import de.code_freak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class CommandLineRunner : EvaluationRunner {

  data class Execution(val command: String, val result: ExecResult) {
    private constructor() : this ("", ExecResult("", 0))
  }

  @Autowired
  private lateinit var containerService: ContainerService

  private val mapper = ObjectMapper()

  override fun getName(): String {
    return "commandline"
  }

  override fun run(answer: Answer, options: Map<String, Any>): String {
    return mapper.writeValueAsString(executeCommands(answer, options, null))
  }

  protected fun executeCommands(answer: Answer, options: Map<String, Any>, processFiles: ((InputStream) -> Unit)?): Execution {
    val image = options.getRequired("image", String::class)
    val projectPath = options.getRequired("project-path", String::class)
    val commands = options.getList("commands", String::class, true)!!
    val stopOnFail = options.get("stop-on-fail", Boolean::class) ?: true
    val command = commands.joinToString("\n")

    containerService.runCommandsForEvaluation(answer, image, projectPath, command, stopOnFail, processFiles).let {
      return Execution(command, it)
    }
  }

  override fun parseResultContent(content: ByteArray): Any {
    return mapper.readValue(content, Execution::class.java)
  }

  override fun getSummary(content: Any): Any {
    return content
  }
}
