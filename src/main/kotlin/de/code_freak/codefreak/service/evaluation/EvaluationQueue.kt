package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.config.EvaluationConfiguration
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
import java.util.Date
import java.util.UUID

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

  private val log = LoggerFactory.getLogger(this::class.java)

  private var queuedEvaluations = mutableSetOf<UUID>()
  private var runningEvaluations = mutableSetOf<UUID>()

  @EventListener(ApplicationStartedEvent::class)
  fun truncatePendingEvaluationsAfterStartup() {
    for (jobInstance in jobExplorer.findJobInstancesByJobName(EvaluationConfiguration.JOB_NAME, 0, Int.MAX_VALUE)) {
      jobExplorer.getJobExecutions(jobInstance).forEach {
        // everything between completed and abandoned will be canceled (starting, started, stopping, stopped)
        if (it.status.isGreaterThan(BatchStatus.COMPLETED) && it.status.isLessThan(BatchStatus.ABANDONED)) {
          log.info("Removing orphaned evaluation for answer ${it.jobParameters.answerId} from database")
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

  fun insert(answerId: UUID) {
    val params = JobParametersBuilder().apply {
      addString(EvaluationConfiguration.PARAM_ANSWER_ID, answerId.toString())
      addDate("date", Date()) // we need this so that we can create a job with the same answer id multiple times
    }.toJobParameters()
    log.debug("Queuing evaluation for answer $answerId")
    queuedEvaluations.add(answerId)
    jobLauncher.run(job, params)
  }

  override fun beforeStep(stepExecution: StepExecution) {
    if (stepExecution.stepName == EvaluationConfiguration.STEP_NAME) {
      stepExecution.jobParameters.answerId?.let {
        queuedEvaluations.remove(it)
        runningEvaluations.add(it)
      }
    }
  }

  override fun afterStep(stepExecution: StepExecution): ExitStatus? {
    if (stepExecution.stepName == EvaluationConfiguration.STEP_NAME) {
      stepExecution.jobParameters.answerId?.let {
        runningEvaluations.remove(it)
      }
    }
    return null
  }

  fun isRunning(answerId: UUID): Boolean = runningEvaluations.contains(answerId)
  fun isQueued(answerId: UUID): Boolean = queuedEvaluations.contains(answerId)

  private val JobParameters.answerId: UUID?
    get() = getString(EvaluationConfiguration.PARAM_ANSWER_ID)?.let { id -> UUID.fromString(id) }
}
