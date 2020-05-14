package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.error
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var evaluationService: EvaluationService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(answer: Answer): Evaluation {
    val digest = fileService.getCollectionMd5Digest(answer.id)
    val evaluation =
        evaluationService.getEvaluationByDigest(answer.id, digest) ?: evaluationService.createEvaluation(answer)
    log.debug("Start evaluation of answer {} ({} steps)", answer.id, answer.task.evaluationStepDefinitions.size)
    answer.task.evaluationStepDefinitions.filter { it.active }.forEach { evaluationStepDefinition ->
      val executedStep = evaluation.evaluationSteps.find { it.definition == evaluationStepDefinition }
      if (executedStep !== null) {
        // Only re-run if this step errored
        if (executedStep.result !== EvaluationStep.EvaluationStepResult.ERRORED) {
          return@forEach
        }
        // remove existing errored step from evaluation for re-running
        evaluation.evaluationSteps.remove(executedStep)
      }
      val runnerName = evaluationStepDefinition.runnerName
      val evaluationStep = EvaluationStep(evaluationStepDefinition)
      try {
        log.debug("Running evaluation step with runner '{}'", runnerName)
        val runner = evaluationService.getEvaluationRunner(runnerName)
        val feedbackList = runner.run(answer, evaluationStepDefinition.options)
        evaluationStep.addAllFeedback(feedbackList)
        // only check for explicitly "failed" feedback so we ignore the "skipped" ones
        if (evaluationStep.feedback.any { feedback -> feedback.isFailed }) {
          evaluationStep.result = EvaluationStep.EvaluationStepResult.FAILED
        } else {
          evaluationStep.result = EvaluationStep.EvaluationStepResult.SUCCESS
        }
        evaluationStep.summary = runner.summarize(feedbackList)
      } catch (e: EvaluationStepException) {
        evaluationStep.result = e.result
        evaluationStep.summary = e.message
      } catch (e: Exception) {
        log.error(e)
        evaluationStep.result = EvaluationStep.EvaluationStepResult.ERRORED
        evaluationStep.summary = e.message ?: "Unknown error"
      }

      log.debug(
          "Step $runnerName finished ${evaluationStep.result}: ${evaluationStep.summary}"
      )
      evaluation.addStep(evaluationStep)
    }
    log.debug("Finished evaluation of answer {}", answer.id)
    return evaluation
  }
}
