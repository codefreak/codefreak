package org.codefreak.codefreak.service.evaluation.runner

import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.evaluation.EvaluationStepException
import org.codefreak.codefreak.util.FileUtil
import org.codefreak.codefreak.util.withTrailingSlash
import org.openmbee.junit.model.JUnitTestCase
import org.openmbee.junit.model.JUnitTestSuite
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
          feedback.addAll(
              testSuiteToFeedback(out.toByteArray())
          )
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

  override fun summarize(feedbackList: List<Feedback>): String {
    val numSuccess = feedbackList.count { feedback -> !feedback.isFailed }
    return "$numSuccess/${feedbackList.size}"
  }

  protected fun testSuiteToFeedback(xmlResult: ByteArray): List<Feedback> {
    val testSuites = try {
      xmlToTestSuites(xmlResult.inputStream())
    } catch (e: Exception) {
      throw EvaluationStepException(
          "Failed to parse jUnit XML:\n${e.message ?: e.cause?.message}",
          result = EvaluationStepResult.ERRORED,
          cause = e
      )
    }

    return testSuites.flatMap { suite ->
      suite.testCases.map { testCase ->
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
  }

  /**
   * jUnit XML allows a <testsuites> root element for multiple <testsuite>s
   * The element CAN be omitted if there is only a single <testsuite>
   */
  private fun xmlToTestSuites(inputStream: InputStream): List<JUnitTestSuite> {
    val root = JAXBContext.newInstance(JUnitTestSuites::class.java, JUnitTestSuite::class.java)
        .createUnmarshaller()
        .unmarshal(inputStream)
    return when (root) {
      is JUnitTestSuites -> root.testSuites
      is JUnitTestSuite -> listOf(root)
      else -> throw RuntimeException("Unexpected root class ${root.javaClass}")
    }
  }

  @XmlRootElement(name = "testsuites")
  class JUnitTestSuites {
    @XmlElement(name = "testsuite")
    lateinit var testSuites: List<JUnitTestSuite>
  }

  val JUnitTestCase.isSkipped
    get() = this.skipped != null
  val JUnitTestCase.isSuccessful
    get() = !isSkipped && this.errors == null && this.failures == null
}
