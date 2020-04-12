package org.codefreak.codefreak.service.evaluation.runner

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.withTrailingSlash
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.service.evaluation.EvaluationStepException
import org.openmbee.junit.JUnitMarshalling
import org.openmbee.junit.model.JUnitTestCase
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import java.io.ByteArrayOutputStream

@Component
class JUnitRunner : CommandLineRunner() {
  override fun getName() = "junit"

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    val resultsPath = options.get("results-path", String::class) ?: "build/test-results/test"
    val resultsPattern = (TarUtil.normalizeEntryName(resultsPath).withTrailingSlash() + "TEST-.+\\.xml").toRegex()
    val defaultOptions = mapOf(
        "image" to "gradle",
        "project-path" to "/home/gradle/project",
        "stop-on-fail" to true,
        "commands" to listOf("gradle testClasses", "gradle test")
    )
    val feedback = mutableListOf<Feedback>()
    super.executeCommands(answer, defaultOptions + options) { files ->
      val tar = TarArchiveInputStream(files)
      generateSequence { tar.nextTarEntry }.forEach {
        if (it.isFile && TarUtil.normalizeEntryName(it.name).matches(resultsPattern)) {
          val out = ByteArrayOutputStream()
          StreamUtils.copy(tar, out)
          feedback.addAll(
              testSuiteToFeedback(out.toByteArray())
          )
        }
      }
    }.also { execution ->
      val firstFailingCommand = execution.find { it.result.exitCode != 0L }
      if (feedback.isEmpty() && firstFailingCommand != null) {
        // e.g. sources failed to compile or other runtime error
        throw EvaluationStepException(
            firstFailingCommand.result.output.trim(),
            EvaluationStep.EvaluationStepResult.FAILED
        )
      }
    }
    return feedback
  }

  override fun summarize(feedbackList: List<Feedback>): String {
    val numSuccess = feedbackList.count { feedback -> !feedback.isFailed }
    return "$numSuccess/${feedbackList.size}"
  }

  protected fun testSuiteToFeedback(xmlResult: ByteArray): List<Feedback> {
    val suite = try {
      JUnitMarshalling.unmarshalTestSuite(xmlResult.inputStream())
    } catch (e: Exception) {
      throw EvaluationStepException(
          "Failed to parse jUnit XML:\n${e.message ?: e.cause?.message}",
          EvaluationStep.EvaluationStepResult.ERRORED,
          e
      )
    }
    return suite.testCases.map { testCase ->
      Feedback(testCase.name).apply {
        group = suite.name
        longDescription = when {
          testCase.failures != null -> testCase.failures.joinToString("\n") { it.message ?: it.value }
          testCase.errors != null -> testCase.errors.joinToString("\n") { it.message ?: it.value }
          else -> null
        }
        // Make jUnit output valid markdown (code block)
        longDescription?.let { longDescription = wrapInMarkdownCodeBlock(it) }
        status = when {
          testCase.isSkipped -> Feedback.Status.IGNORE
          testCase.isSuccessful -> Feedback.Status.SUCCESS
          else -> Feedback.Status.FAILED
        }
        if (status == Feedback.Status.FAILED) {
          severity = if (testCase.errors != null) Feedback.Severity.CRITICAL else Feedback.Severity.MAJOR
        }
      }
    }
  }

  val JUnitTestCase.isSkipped
    get() = this.skipped != null
  val JUnitTestCase.isSuccessful
    get() = !isSkipped && this.errors == null && this.failures == null
}
