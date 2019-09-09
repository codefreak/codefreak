package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationResult
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var evaluationService: EvaluationService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(answer: Answer): Evaluation? {
    val taskDefinition = taskService.getTaskDefinition(answer.task.id)
    log.debug("Start evaluation of answer {} ({} steps)", answer.id, taskDefinition.evaluation.size)
    val results = taskDefinition.evaluation.mapIndexed { index, it ->
      val runnerName = it.step
      try {
        log.debug("Running evaluation step with runner '{}'", runnerName)
        val runner = evaluationService.getEvaluationRunner(runnerName)
        val resultContent = runner.run(answer, it.options)
        println(resultContent)
        EvaluationResult(runnerName, resultContent.toByteArray(), index)
      } catch (e: Exception) {
        log.error(e.message)
        EvaluationResult(runnerName, (e.message ?: "Unknown error").toByteArray(), index, true)
      }
    }
    log.debug("Finished evaluation of answer {}", answer.id)
    return Evaluation(answer, fileService.getCollectionMd5Digest(answer.id), results)
  }
}
