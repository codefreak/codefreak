package de.code_freak.codefreak.service.evaluation.runner

import de.code_freak.codefreak.entity.Answer
import org.springframework.stereotype.Component

@Component
class JUnitRunner : CommandLineRunner() {

  override fun getName(): String {
    return "junit"
  }

  override fun run(answer: Answer, options: Map<String, Any>): String {
    // val resultsPath = options.get("results-path", String::class) ?: "build/test-results/test"
    val defaultOptions = mapOf(
        "image" to "gradle",
        "project-path" to "/home/gradle/project",
        "commands" to listOf("gradle test")
    )
    return super.run(answer, defaultOptions + options)
  }
}
