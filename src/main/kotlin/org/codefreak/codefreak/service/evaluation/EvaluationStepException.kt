package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.EvaluationStepStatus

class EvaluationStepException(
  message: String,
  val result: EvaluationStepResult,
  val status: EvaluationStepStatus? = null,
  cause: Throwable? = null
) : Exception(message, cause)
