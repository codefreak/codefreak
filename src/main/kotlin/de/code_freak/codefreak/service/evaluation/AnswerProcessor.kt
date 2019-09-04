package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.file.FileService
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var containerService: ContainerService

  override fun process(answer: Answer): Evaluation? {
    val output = containerService.runCodeclimate(answer.id)
    println(output)
    println("Writing evaluation result")
    return Evaluation(answer, fileService.getCollectionMd5Digest(answer.id), 5)
  }
}
