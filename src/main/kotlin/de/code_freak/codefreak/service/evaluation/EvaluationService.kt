package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationResult
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.service.BaseService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.UUID

@Service
class EvaluationService : BaseService() {

  @Autowired
  private lateinit var submissionService: SubmissionService

  @Autowired
  private lateinit var evaluationRepository: EvaluationRepository

  @Autowired
  private lateinit var containerService: ContainerService

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var evaluationQueue: EvaluationQueue

  @Autowired
  private lateinit var runners: List<EvaluationRunner>

  private val runnersByName by lazy { runners.map { it.getName() to it }.toMap() }

  private val log = LoggerFactory.getLogger(this::class.java)

  fun startEvaluation(assignment: Assignment) {
    val submissions = submissionService.findSubmissionsOfAssignment(assignment.id)
    submissions.flatMap { it.answers }.map {
      try {
        startEvaluation(it)
      } catch (e: IllegalStateException) {
        // evaluation is already fresh or running
        log.debug("Not queuing evaluation for answer ${it.id}: ${e.message}")
      }
    }
  }

  fun startEvaluation(answer: Answer) {
    containerService.saveAnswerFiles(answer)
    check(!isEvaluationUpToDate(answer.id)) { "Evaluation is up to date." }
    check(!isEvaluationRunningOrQueued(answer.id)) { "Evaluation is already running or queued." }
    evaluationQueue.insert(answer.id)
  }

  fun getLatestEvaluations(answerIds: Iterable<UUID>): Map<UUID, Optional<Evaluation>> {
    return answerIds.map { it to getLatestEvaluation(it) }.toMap()
  }

  fun getLatestEvaluation(answerId: UUID) = evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(answerId)

  fun isEvaluationRunningOrQueued(answerId: UUID) = evaluationQueue.isQueued(answerId) || evaluationQueue.isRunning(answerId)

  fun getSummary(evaluationResult: EvaluationResult): Any {
    return getEvaluationRunner(evaluationResult.runnerName).let {
      it.getSummary(
          it.parseResultContent(evaluationResult.content)
      )
    }
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
