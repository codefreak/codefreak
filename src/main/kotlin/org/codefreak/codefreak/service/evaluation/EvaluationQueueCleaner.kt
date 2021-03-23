package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.config.EvaluationConfiguration
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.util.evaluationStepId
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * If we stop the backend application while evaluations are running their output will get lost
 * This method runs after startup and reschedules evaluation-steps that have not finished yet
 * Our source of truth for unfinished evaluations is the Spring Batch job repository.
 */
@Component
class EvaluationQueueCleaner {

  @Autowired
  private lateinit var jobExplorer: JobExplorer

  @Autowired
  private lateinit var jobOperator: JobOperator

  @Autowired
  private lateinit var evaluationQueue: EvaluationQueue

  @Autowired
  private lateinit var runnerService: EvaluationRunnerService

  @Autowired
  private lateinit var evaluationStepService: EvaluationStepService

  private val log = LoggerFactory.getLogger(this::class.java)

  @EventListener(ApplicationStartedEvent::class)
  fun rescheduleEvaluationsAfterStartup() {
    for (jobInstance in jobExplorer.findJobInstancesByJobName(EvaluationConfiguration.JOB_NAME, 0, Int.MAX_VALUE)) {
      jobExplorer.getJobExecutions(jobInstance).forEach {
        // everything between completed and abandoned will be rescheduled (starting, started, stopping, stopped)
        if (it.status.isGreaterThan(BatchStatus.COMPLETED) && it.status.isLessThan(BatchStatus.ABANDONED)) {
          val stepId = it.jobParameters.evaluationStepId ?: return@forEach
          // at first we mark the job as abandoned in spring batch
          if (it.status.isLessThan(BatchStatus.STOPPING)) {
            // this will 100% write an exception to the log because the job cannot be found but stop() has to be called
            // before we can mark it as "abandoned"
            jobOperator.stop(it.id)
          }
          jobOperator.abandon(it.id)

          // if the step still exist in our database stop the existing evaluation runner and reschedule the single step
          try {
            val evaluationStep = evaluationStepService.getEvaluationStep(stepId)
            runnerService.stopAnswerEvaluation(evaluationStep.definition.runnerName, evaluationStep.evaluation.answer)
            evaluationQueue.insert(evaluationStep)
            log.info("Rescheduled evaluation step $stepId")
          } catch (e: EntityNotFoundException) {
            log.info("Could not find evaluation step $stepId in database. Skip rescheduling")
          }
        }
      }
    }
  }
}
