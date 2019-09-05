package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.config.EvaluationConfiguration
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.service.BaseService
import de.code_freak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.util.Arrays
import java.util.Optional
import java.util.UUID

@Service
class EvaluationService : BaseService() {

  @Autowired
  @EvaluationQualifier
  private lateinit var job: Job

  @Autowired
  @EvaluationQualifier
  private lateinit var jobLauncher: JobLauncher

  @Autowired
  private lateinit var jobExplorer: JobExplorer

  @Autowired
  private lateinit var evaluationRepository: EvaluationRepository

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var runners: List<EvaluationRunner>

  private val runnersByName by lazy { runners.map { it.getName() to it }.toMap() }

  private val log = LoggerFactory.getLogger(this::class.java)

  fun startEvaluation(answerId: UUID) {
    getLatestEvaluation(answerId).ifPresent {
      if (Arrays.equals(it.filesDigest, fileService.getCollectionMd5Digest(answerId))) {
        throw IllegalStateException("Evaluation is up to date")
      }
    }
    if (isEvaluationRunning(answerId)) {
      throw IllegalStateException("Evaluation is already running")
    }
    log.debug("Queuing evaluation for answer {}", answerId)
    val params = mapOf(EvaluationConfiguration.PARAM_ANSWER_ID to JobParameter(answerId.toString()))
    jobLauncher.run(job, JobParameters(params))
  }

  fun getLatestEvaluations(answerIds: Iterable<UUID>): Map<UUID, Optional<Evaluation>> {
    return answerIds.map { it to getLatestEvaluation(it) }.toMap()
  }

  fun getLatestEvaluation(answerId: UUID) = evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(answerId)

  fun isEvaluationRunning(answerId: UUID): Boolean {
    val id = answerId.toString()
    for (execution in jobExplorer.findRunningJobExecutions(EvaluationConfiguration.JOB_NAME)) {
      if (id == execution.jobParameters.getString(EvaluationConfiguration.PARAM_ANSWER_ID)) {
        return true
      }
    }
    return false
  }

  fun getEvaluationRunner(name: String): EvaluationRunner = runnersByName[name] ?:
      throw IllegalArgumentException("Evaluation runner '$name' not found")
}
