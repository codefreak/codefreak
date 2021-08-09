package org.codefreak.codefreak.service.evaluation

import java.io.InputStream
import org.codefreak.codefreak.entity.Feedback

/**
 * Parsers can convert arbitrary formats into Code FREAK Feedback
 */
interface EvaluationReportFormatParser {
  val id: String
  fun parse(input: String): List<Feedback> = parse(input.byteInputStream())
  fun parse(input: InputStream): List<Feedback>
  fun summarize(feedbackList: List<Feedback>): String
}
