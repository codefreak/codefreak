package org.codefreak.codefreak.service.evaluation

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStep.EvaluationStepResult
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Component

@Component
class AnswerProcessor : ItemProcessor<Answer, Evaluation> {

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  @Qualifier("asyncTaskExecutor")
  private lateinit var taskExecutor: AsyncTaskExecutor

  @Autowired
  private lateinit var evaluationService: EvaluationService

  @Value("#{@config.evaluation.defaultTimeout}")
  private var defaultTimeout: Long = 0L

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(answer: Answer): Evaluation {
    val digest = fileService.getCollectionMd5Digest(answer.id)
    val evaluation = evaluationService.getOrCreateValidEvaluationByDigest(answer, digest)
    log.debug("Start evaluation of answer {} ({} steps)", answer.id, answer.task.evaluationStepDefinitions.size)
    answer.task.evaluationStepDefinitions.filter { it.active }.forEach { evaluationStepDefinition ->
      val executedStep = evaluation.evaluationSteps.find { it.definition == evaluationStepDefinition }
      if (executedStep !== null) {
        // Only re-run if this step errored
        if (executedStep.result !== EvaluationStepResult.ERRORED) {
          return@forEach
        }
        // remove existing errored step from evaluation for re-running
        evaluation.evaluationSteps.remove(executedStep)
      }
      val runnerName = evaluationStepDefinition.runnerName
      val evaluationStep = EvaluationStep(evaluationStepDefinition, evaluation)
      try {
        val runner = evaluationService.getEvaluationRunner(runnerName)
        val feedbackList = runEvaluation(runner, evaluationStep)
        evaluationStep.addAllFeedback(feedbackList)
        // only check for explicitly "failed" feedback so we ignore the "skipped" ones
        if (evaluationStep.feedback.any { feedback -> feedback.isFailed }) {
          evaluationStep.result = EvaluationStepResult.FAILED
        } else {
          evaluationStep.result = EvaluationStepResult.SUCCESS
        }
        evaluationStep.summary = runner.summarize(feedbackList)
      } catch (e: EvaluationStepException) {
        evaluationStep.result = e.result
        evaluationStep.summary = e.message
      } catch (e: Exception) {
        log.error("Evaluation step $runnerName of answer ${answer.id} failed unexpectedly:", e)
        evaluationStep.result = EvaluationStepResult.ERRORED
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

  private fun runEvaluation(runner: EvaluationRunner, step: EvaluationStep): List<Feedback> {
    val answer = step.evaluation.answer
    val timeout = step.definition.timeout ?: defaultTimeout
    val runnerName = runner.getName()
    try {
      val task = taskExecutor.submit(Callable {
        runner.run(answer, step.definition.options)
      })
      return if (timeout > 0) {
        log.debug("Running evaluation step $runnerName of answer ${answer.id}  with a time limit of $timeout seconds")
        task.get(timeout, TimeUnit.SECONDS)
      } else {
        log.info("Running evaluation step $runnerName of answer ${answer.id} without time limit!")
        task.get()
      }
    } catch (e: TimeoutException) {
      log.info("Timeout for evaluation step $runnerName of answer ${answer.id} occurred after ${timeout}sec")
      evaluationService.stopEvaluation(answer, runner.getName())
      throw EvaluationStepException("Evaluation timed out after $timeout seconds", EvaluationStepResult.ERRORED)
    } catch (e: ExecutionException) {
      // unwrap exceptions from EvaluationRunner#run() to catch them properly
      throw e.cause ?: e
    }
  }
}