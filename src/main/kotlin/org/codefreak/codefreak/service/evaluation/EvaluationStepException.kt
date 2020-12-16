package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStepResult

class EvaluationStepException(message: String, val result: EvaluationStepResult, cause: Throwable? = null) : Exception(message, cause)
