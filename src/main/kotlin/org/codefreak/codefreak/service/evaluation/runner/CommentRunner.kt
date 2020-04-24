package org.codefreak.codefreak.service.evaluation.runner

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.stereotype.Component

@Component
class CommentRunner : EvaluationRunner {
  companion object {
    const val RUNNER_NAME = "comments"
  }

  override fun getName() = RUNNER_NAME

  override fun getDefaultTitle() = "Comments"

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    // Does nothing automatically. Comments are added via frontend.
    // Maybe in the future we will have some form of notification for the teacher or an AI that creates comments
    return listOf()
  }
}
