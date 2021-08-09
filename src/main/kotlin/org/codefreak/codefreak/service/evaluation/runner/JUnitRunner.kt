package org.codefreak.codefreak.service.evaluation.runner

import java.io.ByteArrayOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.evaluation.EvaluationStepException
import org.codefreak.codefreak.service.evaluation.JunitXmlFormatParser
import org.codefreak.codefreak.util.FileUtil
import org.codefreak.codefreak.util.withTrailingSlash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils

@Component
class JUnitRunner : CommandLineRunner() {
  override fun getName() = "junit"

  override fun getDefaultTitle() = "Unit Tests"

  override fun getDocumentationUrl() = "https://docs.codefreak.org/codefreak/for-teachers/definitions.html#junit"

  override fun getOptionsSchema() = ClassPathResource("evaluation/junit.schema.json").inputStream.use { String(it.readBytes()) }

  override fun getDefaultOptions() = mapOf(
    "image" to "gradle",
    "project-path" to "/home/gradle/project",
    "stop-on-fail" to true,
    "commands" to listOf("gradle testClasses", "gradle test")
  )

  @Autowired
  private lateinit var junitXmlFormatParser: JunitXmlFormatParser

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    val resultsPath = options.get("results-path", String::class) ?: "build/test-results/test"
    val resultsPattern = FileUtil.sanitizeName(resultsPath.withTrailingSlash() + "TEST-.+\\.xml").toRegex()
    val defaultOptions = getDefaultOptions()
    val feedback = mutableListOf<Feedback>()
    super.executeCommands(answer, defaultOptions + options) { files ->
      val tar = TarArchiveInputStream(files)
      generateSequence { tar.nextTarEntry }.forEach {
        if (it.isFile && FileUtil.sanitizeName(it.name).matches(resultsPattern)) {
          val out = ByteArrayOutputStream()
          StreamUtils.copy(tar, out)
          feedback.addAll(junitXmlFormatParser.parse(out.toByteArray().inputStream()))
        }
      }
    }.also { execution ->
      // no feedback means the sources failed to compile
      val firstFailingCommand = execution.find { it.result.exitCode != 0L }
      if (feedback.isEmpty() && firstFailingCommand != null) {
        // e.g. sources failed to compile or other runtime error
        throw EvaluationStepException(
            firstFailingCommand.result.output.trim(),
            EvaluationStepResult.FAILED
        )
      }
    }
    return feedback
  }

  override fun summarize(feedbackList: List<Feedback>) = junitXmlFormatParser.summarize(feedbackList)
}
