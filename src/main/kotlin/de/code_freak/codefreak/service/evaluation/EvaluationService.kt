package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.config.EvaluationConfiguration
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationResult
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.service.BaseService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date
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
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var runners: List<EvaluationRunner>

  private val runnersByName by lazy { runners.map { it.getName() to it }.toMap() }

  private val log = LoggerFactory.getLogger(this::class.java)

  fun startEvaluation(answer: Answer) {
    containerService.saveAnswerFiles(answer)
    check(!isEvaluationUpToDate(answer.id)) { "Evaluation is up to date." }
    check(!isEvaluationRunning(answer.id)) { "Evaluation is already running." }
    log.debug("Queuing evaluation for answer {}", answer.id)
    val params = JobParametersBuilder().apply {
      addString(EvaluationConfiguration.PARAM_ANSWER_ID, answer.id.toString())
      addDate("date", Date()) // we need this so that we can create a job with the same answer id multiple times
    }.toJobParameters()
    jobLauncher.run(job, params)
  }

  fun getLatestEvaluations(answerIds: Iterable<UUID>): Map<UUID, Optional<Evaluation>> {
    return answerIds.map { it to getLatestEvaluation(it) }.toMap()
  }

  fun getLatestEvaluation(answerId: UUID) = evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(answerId)

  fun isEvaluationRunning(answerId: UUID): Boolean {
    val id = answerId.toString()
    for (jobInstance in jobExplorer.findJobInstancesByJobName(EvaluationConfiguration.JOB_NAME, 0, Int.MAX_VALUE)) {
      jobExplorer.getJobExecutions(jobInstance).forEach {
        if (id == it.jobParameters.getString(EvaluationConfiguration.PARAM_ANSWER_ID)) {
          if (it.status == BatchStatus.STARTED || it.status == BatchStatus.STARTING) {
            return true
          }
        }
      }
    }
    return false
  }

  fun isEvaluationUpToDate(answerId: UUID): Boolean = getLatestEvaluation(answerId).map {
    it.filesDigest.contentEquals(fileService.getCollectionMd5Digest(answerId))
  }.orElse(false)

  fun getEvaluationRunner(name: String): EvaluationRunner = runnersByName[name]
      ?: throw IllegalArgumentException("Evaluation runner '$name' not found")

  fun getEvaluation(evaluationId: UUID): Evaluation {
    return evaluationRepository.findById(evaluationId).orElseThrow { EntityNotFoundException("Evaluation not found") }
  }
}
