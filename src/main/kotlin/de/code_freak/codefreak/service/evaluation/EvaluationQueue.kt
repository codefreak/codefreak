package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.config.EvaluationConfiguration
import org.slf4j.LoggerFactory
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
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

  private val log = LoggerFactory.getLogger(this::class.java)

  private var queuedEvaluations = mutableSetOf<UUID>()
  private var runningEvaluations = mutableSetOf<UUID>()

  fun insert(answerId: UUID) {
    val params = JobParametersBuilder().apply {
      addString(EvaluationConfiguration.PARAM_ANSWER_ID, answerId.toString())
      addDate("date", Date()) // we need this so that we can create a job with the same answer id multiple times
    }.toJobParameters()
    log.debug("Queuing evaluation for answer $answerId")
    jobLauncher.run(job, params)
    queuedEvaluations.add(answerId)
  }

  override fun beforeStep(stepExecution: StepExecution) {
    if (stepExecution.stepName == EvaluationConfiguration.STEP_NAME) {
      stepExecution.answerId?.let {
        queuedEvaluations.remove(it)
        runningEvaluations.add(it)
      }
    }
  }

  override fun afterStep(stepExecution: StepExecution): ExitStatus? {
    if (stepExecution.stepName == EvaluationConfiguration.STEP_NAME) {
      stepExecution.answerId?.let {
        runningEvaluations.remove(it)
      }
    }
    return null
  }

  fun isRunning(answerId: UUID): Boolean = runningEvaluations.contains(answerId)
  fun isQueued(answerId: UUID): Boolean = queuedEvaluations.contains(answerId)

  private val StepExecution.answerId: UUID?
    get() = jobParameters.getString(EvaluationConfiguration.PARAM_ANSWER_ID)?.let { id -> UUID.fromString(id) }
}
