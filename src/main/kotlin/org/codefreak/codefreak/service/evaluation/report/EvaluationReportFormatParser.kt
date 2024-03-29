package org.codefreak.codefreak.service.evaluation.report

import java.io.InputStream
import org.codefreak.codefreak.entity.Feedback

/**
 * Parsers can convert arbitrary formats into Code FREAK Feedback
 */
interface EvaluationReportFormatParser {
  val id: String
  val title: String

  /**
   * Parse the given file content InputStream into a list of Feedback.
   * The implementation should NOT close the underlying input stream!
   */
  fun parse(fileContent: InputStream): List<Feedback>
  fun parse(fileContent: String): List<Feedback> = parse(fileContent.byteInputStream())

  /**
   * Summarize a list of feedback that has been generated by this report parser.
   * The resulting string should give a quick overview about the feedback e.g. a number of failed unit tests
   * or a summary of format violations.
   * By default, this will show the number of violations in each severity group or an empty string if no feedback.
   */
  fun summarize(feedbackList: List<Feedback>): String {
    return feedbackList.groupBy { it.severity }.entries.joinToString { (k, v) -> "${v.size}x $k" }
  }
}
