package org.codefreak.codefreak.service.evaluation.report

import java.io.InputStream
import javax.xml.bind.JAXBContext
import javax.xml.bind.UnmarshalException
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.util.wrapInMarkdownCodeBlock
import org.openmbee.junit.model.JUnitTestCase
import org.openmbee.junit.model.JUnitTestSuite
import org.springframework.stereotype.Component

/**
 * Parses jUnit XML format. There is no official standard for this format.
 * Check this SO post for some gathered information:
 * https://stackoverflow.com/questions/4922867/what-is-the-junit-xml-format-specification-that-hudson-supports
 */
@Component
class JunitXmlFormatParser : EvaluationReportFormatParser {
  override val id = "junit-xml"
  override val title = "jUnit XML"

  private val unmarshaller = JAXBContext.newInstance(JUnitTestSuites::class.java, JUnitTestSuite::class.java).createUnmarshaller()

  override fun parse(fileContent: InputStream): List<Feedback> = testSuiteToFeedback(fileContent)

  override fun summarize(feedbackList: List<Feedback>): String {
    val numSuccess = feedbackList.count { feedback -> !feedback.isFailed }
    return "$numSuccess/${feedbackList.size}"
  }

  protected fun testSuiteToFeedback(xmlResult: InputStream): List<Feedback> {
    val testSuites = try {
      xmlToTestSuites(xmlResult)
    } catch (e: RuntimeException) {
      throw EvaluationReportParsingException("Failed to parseStdout jUnit XML:\n${e.message ?: e.cause?.message}", e)
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
          longDescription?.let { longDescription = it.wrapInMarkdownCodeBlock() }
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
    try {
      return when (val root = unmarshaller.unmarshal(inputStream)) {
        is JUnitTestSuites -> root.testSuites
        is JUnitTestSuite -> listOf(root)
        else -> throw EvaluationReportParsingException("Unexpected root element ${root.javaClass}. Expected <testsuites> or <testsuite>.")
      }
    } catch (e: UnmarshalException) {
      throw EvaluationReportParsingException("Could not parseStdout content as XML")
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
