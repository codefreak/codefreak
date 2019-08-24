package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.service.BaseService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
  private lateinit var evaluationRepository: EvaluationRepository

  private val log = LoggerFactory.getLogger(this::class.java)

  fun queueEvaluation(answerId: UUID) {
    log.debug("Queuing evaluation for answer {}", answerId)
    val params = mapOf("answerId" to JobParameter(answerId.toString()))
    jobLauncher.run(job, JobParameters(params))
  }

  fun getLatestEvaluations(answerIds: Iterable<UUID>): Map<UUID, Optional<Evaluation>> {
    return answerIds.map { it to evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(it) }.toMap()
  }
}
