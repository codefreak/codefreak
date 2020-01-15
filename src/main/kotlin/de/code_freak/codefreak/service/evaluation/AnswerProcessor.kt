package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationStep
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.error
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var evaluationService: EvaluationService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(answer: Answer): Evaluation {
    val taskDefinition = taskService.getTaskDefinition(answer.task.id)
    log.debug("Start evaluation of answer {} ({} steps)", answer.id, taskDefinition.evaluation.size)
    val evaluation = Evaluation(answer, fileService.getCollectionMd5Digest(answer.id))
    taskDefinition.evaluation.forEachIndexed { index, evaluationDefinition ->
      val runnerName = evaluationDefinition.step
      val evaluationStep = EvaluationStep(runnerName, index)
      try {
        log.debug("Running evaluation step with runner '{}'", runnerName)
        val runner = evaluationService.getEvaluationRunner(runnerName)
        val feedbackList = runner.run(answer, evaluationDefinition.options)
        evaluationStep.addAllFeedback(feedbackList)
        // only check for explicitly "failed" feedback so we ignore the "skipped" ones
        if (evaluationStep.feedback.any { feedback -> feedback.isFailed }) {
          evaluationStep.result = EvaluationStep.EvaluationStepResult.FAILED
        } else {
          evaluationStep.result = EvaluationStep.EvaluationStepResult.SUCCESS
        }
        evaluationStep.summary = runner.summarize(feedbackList)
      } catch (e: Exception) {
        log.error(e)
        evaluationStep.result = EvaluationStep.EvaluationStepResult.ERRORED
        evaluationStep.summary = e.message ?: "Unknown error"
      }

      evaluation.addStep(evaluationStep)
    }
    log.debug("Finished evaluation of answer {}", answer.id)
    return evaluation
  }
}
