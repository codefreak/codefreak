package org.codefreak.codefreak.service.evaluation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FormatParserRegistry(
  @Autowired parsers: List<EvaluationReportFormatParser>
) {
  private val parserMap = parsers.associateBy { it.id }

  fun getParser(format: String) = parserMap[format]
      ?: throw IllegalArgumentException("Invalid parser specified: $format. Supported parsers are ${parserMap.keys.joinToString()}")
}
