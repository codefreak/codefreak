package org.codefreak.codefreak.service.evaluation

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepResult
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.entity.Feedback
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Component

@Component
class EvaluationStepProcessor : ItemProcessor<EvaluationStep, EvaluationStep?> {

  @Autowired
  @Qualifier("asyncTaskExecutor")
  private lateinit var taskExecutor: AsyncTaskExecutor

  @Autowired
  private lateinit var evaluationStepService: EvaluationStepService

  @Autowired
  private lateinit var runnerService: EvaluationRunnerService

  @Value("#{@config.evaluation.defaultTimeout}")
  private var defaultTimeout: Long = 0L

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun process(evaluationStep: EvaluationStep): EvaluationStep? {
    val answer = evaluationStep.evaluation.answer
    log.debug("Start evaluation of answer {} ({} steps)", answer.id, answer.task.evaluationStepDefinitions.size)

    val stepDefinition = evaluationStep.definition
    val runnerName = stepDefinition.runnerName
    try {
      val runner = runnerService.getEvaluationRunner(runnerName)
      val feedbackList = try {
        runEvaluation(runner, evaluationStep)
      } catch (e: InterruptedException) {
        // happens if the application shuts down. In this case we will stop any further processing.
        // The evaluation will be re-run if the application restarts
        return null
      }
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
      if (e.status != null) {
        evaluationStep.status = e.status
      }
    } catch (e: Exception) {
      log.error("Evaluation step $runnerName of answer ${answer.id} failed unexpectedly:", e)
      evaluationStep.result = EvaluationStepResult.ERRORED
      evaluationStep.summary = e.message ?: "Unknown error"
    }

    log.debug(
        "Step $runnerName finished ${evaluationStep.result}: ${evaluationStep.summary}"
    )
    return evaluationStep
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
      runnerService.stopAnswerEvaluation(runnerName, answer)
      throw EvaluationStepException("Evaluation timed out after $timeout seconds",
          result = EvaluationStepResult.ERRORED,
          status = EvaluationStepStatus.CANCELED
      )
    } catch (e: ExecutionException) {
      // unwrap exceptions from EvaluationRunner#run() to catch them properly
      throw e.cause ?: e
    }
  }
}
