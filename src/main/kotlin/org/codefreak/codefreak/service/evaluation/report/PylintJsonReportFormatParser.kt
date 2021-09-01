package org.codefreak.codefreak.service.evaluation.report

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream
import org.codefreak.codefreak.entity.Feedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PylintJsonReportFormatParser(
  @Autowired private val jsonObjectMapper: ObjectMapper
) : EvaluationReportFormatParser {
  override val id = "pylint-json"
  override val title = "Pylint JSON"

  override fun parse(fileContent: InputStream): List<Feedback> {
    return jsonObjectMapper.readValue(fileContent, Array<PylintViolation>::class.java).map(::pylintViolationToFeedback)
  }

  private fun pylintViolationToFeedback(violation: PylintViolation): Feedback {
    return Feedback(summary = violation.message).apply {
      status = Feedback.Status.FAILED
      severity = if (violation.type == "convention") Feedback.Severity.MINOR else Feedback.Severity.MAJOR
      group = violation.symbol
      fileContext = Feedback.FileContext(
          path = violation.path,
          lineStart = violation.line,
          columnStart = violation.column
      )
    }
  }

  data class PylintViolation(
    val type: String,
    val module: String,
    val obj: String,
    val line: Int,
    val column: Int,
    val path: String,
    val symbol: String,
    val message: String,
    @JsonProperty("message-id")
    val messageId: String
  )
}
