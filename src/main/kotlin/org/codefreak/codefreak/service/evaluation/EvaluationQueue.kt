package org.codefreak.codefreak.service.evaluation

import java.time.Instant
import java.util.Date
import org.codefreak.codefreak.config.EvaluationConfiguration
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.codefreak.codefreak.util.evaluationStepId
import org.slf4j.LoggerFactory
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
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
  private lateinit var evaluationStepService: EvaluationStepService

  private val log = LoggerFactory.getLogger(this::class.java)

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
  fun insert(evaluation: Evaluation) = evaluation.evaluationSteps.forEach {
    insert(it)
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  fun insert(step: EvaluationStep) {
    log.debug("Queuing evaluation step ${step.definition.runnerName} of answer ${step.evaluation.answer.id}")
    val params = buildJobParameters(step)
    jobLauncher.run(job, params)
    step.queuedAt = Instant.now()
    evaluationStepService.updateEvaluationStepStatus(step, EvaluationStepStatus.QUEUED)
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
        evaluationStep.finishedAt = Instant.now()
        evaluationStepService.updateEvaluationStepStatus(evaluationStep, status)
      }
    }
    return null
  }
}
