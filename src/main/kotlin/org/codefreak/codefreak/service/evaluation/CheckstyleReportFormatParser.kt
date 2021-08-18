package org.codefreak.codefreak.service.evaluation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import java.io.InputStream
import org.codefreak.codefreak.entity.Feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * Parser that can convert Checkstyle's XML to a list of feedback.
 * As always there is no real standard or definition for this format but checkstyle provides some example
 * how the xml files will look like.
 * The format can also be written by various other tools like ktlint (Kotlin), PHP_CodeSniffer or eslint (JavaScript).
 *
 * @see <a href="https://github.com/checkstyle/checkstyle">Checkstyle GitHub Repository</a>
 * @see <a href="https://github.com/checkstyle/checkstyle/tree/master/src/test/resources/com/puppycrawl/tools/checkstyle/xmllogger">Checkstyle XML test case examples</a>
 */
@Component
class CheckstyleReportFormatParser(
  @Autowired
  @Qualifier("xmlObjectMapper")
  val xmlMapper: ObjectMapper
) : EvaluationReportFormatParser {
  override val id = "checkstyle-xml"
  override val title = "Checkstyle XML"

  /**
   * Map from checkstyle severity levels to our Feedback severity levels.
   *
   * @see <a href="https://github.com/checkstyle/checkstyle/blob/master/src/main/java/com/puppycrawl/tools/checkstyle/api/SeverityLevel.java">Checkstyle Severity Levels</a>
   */
  private val severityMap = mapOf(
      "ignore" to Feedback.Severity.INFO,
      "info" to Feedback.Severity.INFO,
      "warning" to Feedback.Severity.MAJOR,
      "error" to Feedback.Severity.CRITICAL
  )
  private val defaultSeverity = Feedback.Severity.MINOR

  override fun parse(exitCode: Int, stdout: String, fileContent: InputStream): List<Feedback> {
    return when (val checkstyleRoot = xmlMapper.readValue(fileContent, CheckstyleXmlRoot::class.java)) {
      is CheckstyleXmlRoot -> checkstyleRootToFeedback(checkstyleRoot)
      else -> throw EvaluationReportParsingException("Expected a root element of <checkstyle> but received $checkstyleRoot instead")
    }
  }

  private fun checkstyleRootToFeedback(checkstyleRoot: CheckstyleXmlRoot): List<Feedback> {
    return when {
      checkstyleRoot.files != null -> checkstyleRoot.files.flatMap { checkstyleFileToFeedback(it) }
      checkstyleRoot.errors != null -> checkstyleRoot.errors.map { checkstyleErrorToFeedback(it) }
      checkstyleRoot.exception != null -> listOf(checkstyleExceptionToFeedback(checkstyleRoot.exception))
      else -> throw EvaluationReportParsingException("Empty checkstyle report")
    }
  }

  private fun checkstyleErrorToFeedback(error: CheckstyleError, fileName: String? = null): Feedback {
    return Feedback(summary = error.message).apply {
      status = Feedback.Status.FAILED
      severity = severityMap[error.severity] ?: defaultSeverity
      group = error.source
      if (fileName != null) {
        fileContext = Feedback.FileContext(
            path = fileName,
            lineStart = error.line,
            columnStart = error.column
        )
      }
    }
  }

  private fun checkstyleFileToFeedback(file: CheckstyleFile): List<Feedback> {
    return when {
      file.errors != null -> file.errors.map { checkstyleErrorToFeedback(it, file.name) }
      file.exception != null -> listOf(checkstyleExceptionToFeedback(file.exception, file.name))
      else -> emptyList()
    }
  }

  private fun checkstyleExceptionToFeedback(exception: String, fileName: String? = null): Feedback {
    val summary = if (fileName != null) "Exception in file $fileName" else "Failed to compile"
    return Feedback(summary = summary).apply {
      longDescription = exception.trim()
      status = Feedback.Status.FAILED
      severity = Feedback.Severity.CRITICAL
      if (fileName != null) {
        fileContext = Feedback.FileContext(
            path = fileName
        )
      }
    }
  }

  override fun summarize(feedbackList: List<Feedback>): String {
    return feedbackList.groupBy { it.severity }.entries.joinToString { (k, v) -> "${v.size}x $k" }
  }

  data class CheckstyleXmlRoot(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty("file")
    val files: List<CheckstyleFile>?,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty("error")
    val errors: List<CheckstyleError>?,
    @JacksonXmlCData
    val exception: String?
  )

  data class CheckstyleFile(
    val name: String?,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JsonProperty("error")
    val errors: List<CheckstyleError>?,
    @JacksonXmlElementWrapper(useWrapping = false)
    val exception: String?
  )

  data class CheckstyleError(
    val line: Int,
    val severity: String,
    val message: String,
    val source: String,
    val column: Int?
  )
}
