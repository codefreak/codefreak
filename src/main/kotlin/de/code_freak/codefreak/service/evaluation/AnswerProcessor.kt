package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {
  override fun process(answer: Answer): Evaluation? {
    try {
        Thread.sleep(5000)
    } catch (e: InterruptedException) {}
    return Evaluation(answer, 5)
  }
}
