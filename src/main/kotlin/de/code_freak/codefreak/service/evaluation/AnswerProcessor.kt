package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.service.file.FileService
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {

  @Autowired
  private lateinit var fileService: FileService

  override fun process(answer: Answer): Evaluation? {
    try {
      println("Executing evaluation")
      Thread.sleep(5000)
    } catch (e: InterruptedException) {}
    println("Writing evaluation result")
    return Evaluation(answer, fileService.getCollectionMd5Digest(answer.id), 5)
  }
}
