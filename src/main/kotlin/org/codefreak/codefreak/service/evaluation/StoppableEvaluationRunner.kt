package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.Answer

interface StoppableEvaluationRunner : EvaluationRunner {
  fun stop(answer: Answer)
}
