package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.EvaluationStep

class EvaluationStepException(message: String, val result: EvaluationStep.EvaluationStepResult, cause: Throwable? = null) : Exception(message, cause)
