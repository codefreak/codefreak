package de.code_freak.codefreak.service.evaluation.runner

import com.fasterxml.jackson.databind.ObjectMapper
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.ExecResult
import de.code_freak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CommandLineRunner : EvaluationRunner {

  companion object {
    protected data class Execution(val command: String, val result: ExecResult) {
      private constructor() : this ("", ExecResult("", 0))
    }
  }

  @Autowired
  private lateinit var containerService: ContainerService

  private val mapper = ObjectMapper()

  override fun getName(): String {
    return "commandline"
  }

  override fun run(answer: Answer, options: Map<String, Any>): String {
    val image = options.getRequired("image", String::class)
    val projectPath = options.getRequired("project-path", String::class)
    val commands = options.getList("commands", String::class, true)!!

    val results = containerService.runCommandsForEvaluation(answer, image, projectPath, commands.toList())
        .mapIndexed { index, result -> Execution(commands[index], result) }

    return mapper.writeValueAsString(results)
  }

  override fun parseResultContent(content: ByteArray): Any {
    return mapper.readValue(content, Array<Execution>::class.java)
  }
}
