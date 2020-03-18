package de.code_freak.codefreak.service.evaluation.runner

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Feedback
import de.code_freak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.stereotype.Component

@Component
class CommentRunner : EvaluationRunner {
  override fun getName(): String {
    return "comments"
  }

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    // Does nothing automatically. Comments are added via frontend.
    // Maybe in the future we will have some form of notification for the teacher or an AI that creates comments
    return listOf()
  }
}