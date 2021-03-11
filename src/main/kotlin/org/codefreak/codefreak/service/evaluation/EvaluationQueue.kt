package org.codefreak.codefreak.service.evaluation

import java.util.Date
import java.util.UUID
import org.codefreak.codefreak.config.EvaluationConfiguration
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class EvaluationQueue : StepExecutionListener {

  @Autowired
  @EvaluationQualifier
  private lateinit var job: Job

  @Autowired
  @EvaluationQualifier
  private lateinit var jobLauncher: JobLauncher

  @Autowired
  private lateinit var jobExplorer: JobExplorer

  @Autowired
  private lateinit var jobOperator: JobOperator

  @Autowired
  private lateinit var evaluationStepService: EvaluationStepService

  private val log = LoggerFactory.getLogger(this::class.java)

  @EventListener(ApplicationStartedEvent::class)
  fun truncatePendingEvaluationsAfterStartup() {
    for (jobInstance in jobExplorer.findJobInstancesByJobName(EvaluationConfiguration.JOB_NAME, 0, Int.MAX_VALUE)) {
      jobExplorer.getJobExecutions(jobInstance).forEach {
        // everything between completed and abandoned will be canceled (starting, started, stopping, stopped)
        if (it.status.isGreaterThan(BatchStatus.COMPLETED) && it.status.isLessThan(BatchStatus.ABANDONED)) {
          log.info("Removing orphaned evaluation for step ${it.jobParameters.evaluationStepId} from database")
          if (it.status.isLessThan(BatchStatus.STOPPING)) {
            // this will 100% write an exception to the log because the job cannot be found but stop() has to be called
            // before we can mark it as "abandoned"
            jobOperator.stop(it.id)
          }
          jobOperator.abandon(it.id)
        }
      }
    }
  }

  private fun buildJobParameters(evaluationStep: EvaluationStep): JobParameters {
    return JobParametersBuilder().apply {
      addString(EvaluationConfiguration.PARAM_EVALUATION_STEP_ID, evaluationStep.id.toString())
      // we need this so that we can create a job with the same answer id multiple times
      addDate("date", Date())
    }.toJobParameters()
  }

  /**
   * Mark as Propagation.NOT_SUPPORTED to prevent exceptions from JobRepository
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  fun insert(evaluation: Evaluation) {
    val answerId = evaluation.answer.id
    for (step in evaluation.evaluationSteps) {
      log.debug("Queuing evaluation step ${step.definition.runnerName} for answer $answerId")
      val params = buildJobParameters(step)
      jobLauncher.run(job, params)
      evaluationStepService.updateEvaluationStepStatus(step, EvaluationStepStatus.QUEUED)
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  override fun beforeStep(stepExecution: StepExecution) {
    if (stepExecution.stepName == EvaluationConfiguration.STEP_NAME) {
      stepExecution.jobParameters.evaluationStepId?.let {
        evaluationStepService.updateEvaluationStepStatus(it, EvaluationStepStatus.RUNNING)
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  override fun afterStep(stepExecution: StepExecution): ExitStatus? {
    if (stepExecution.stepName == EvaluationConfiguration.STEP_NAME) {
      stepExecution.jobParameters.evaluationStepId?.let {
        val evaluationStep = evaluationStepService.getEvaluationStep(it)
        // keep the finished/canceled status that may have been set by the runner
        val status = when {
          evaluationStep.status >= EvaluationStepStatus.FINISHED -> evaluationStep.status
          else -> EvaluationStepStatus.FINISHED
        }
        // retrieve a fresh instance of the current EvaluationStep. Start Autograding.
        // This function needs a call right before updateEvaluationStepStatus, because afterwards there
        // might be a GradeCalculation
        evaluationStepService.startAutograding(evaluationStepService.getEvaluationStep(it))

        evaluationStepService.updateEvaluationStepStatus(evaluationStep, status)
      }
    }
    return null
  }

  private val JobParameters.evaluationStepId: UUID?
    get() = getString(EvaluationConfiguration.PARAM_EVALUATION_STEP_ID)?.let { id -> UUID.fromString(id) }
}
