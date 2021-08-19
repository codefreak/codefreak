package org.codefreak.codefreak.service.evaluation.report

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class FormatParserRegistry(
  @Autowired parsers: List<EvaluationReportFormatParser>
) {
  val allParsers = parsers
  private val parserMap = parsers.associateBy { it.id }

  fun getParser(format: String): EvaluationReportFormatParser {
    if (format.isBlank()) {
      throw IllegalArgumentException("No format parser specified. Supported parsers are ${parserMap.keys.joinToString()}")
    }
    return parserMap[format]
        ?: throw IllegalArgumentException("Invalid parser specified: $format. Supported parsers are ${parserMap.keys.joinToString()}")
  }
}
