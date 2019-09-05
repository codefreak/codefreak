package de.code_freak.codefreak.service.evaluation

import java.util.UUID

interface EvaluationRunner {
  fun getName(): String
  fun run(answerId: UUID, options: Map<String, Any>): String
  fun parseResultContent(content: ByteArray): Any
}
