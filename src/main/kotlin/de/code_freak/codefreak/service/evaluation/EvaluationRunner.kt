package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer

interface EvaluationRunner {
  fun getName(): String
  fun run(answer: Answer, options: Map<String, Any>): String
  fun parseResultContent(content: ByteArray): Any
}
