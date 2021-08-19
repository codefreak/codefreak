package org.codefreak.codefreak.service.evaluation.report

import java.io.InputStream
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.util.wrapInMarkdownCodeBlock
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils

/**
 * This is a default "best effort" evaluation report parser.
 * The parser should not be used in real-world tests but give first insights when doing quick try&error scenarios.
 * It shows the content of a report file (or stdout in case no file was given) and the exit code as summary.
 */
@Component
class DefaultReportFormatParser : EvaluationReportFormatParser {
  override val id = "default"
  override val title = "Display Output"

  override fun parse(fileContent: InputStream): List<Feedback> {
    val outputString = StreamUtils.copyToString(fileContent, Charsets.UTF_8)
    val feedback = Feedback("Console Output").also {
      it.longDescription = outputString.trim().wrapInMarkdownCodeBlock()
    }
    return listOf(feedback)
  }

  override fun summarize(feedbackList: List<Feedback>) = feedbackList.firstOrNull()?.summary ?: "No output generated"
}
