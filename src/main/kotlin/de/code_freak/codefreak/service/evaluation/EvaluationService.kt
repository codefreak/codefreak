package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.service.BaseService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.service.file.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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

  fun startEvaluation(assignment: Assignment): List<Answer> {
    val submissions = submissionService.findSubmissionsOfAssignment(assignment.id)
    return submissions.flatMap { it.answers }.mapNotNull {
      try {
        startEvaluation(it)
        it
      } catch (e: IllegalStateException) {
        // evaluation is already fresh or running
        log.debug("Not queuing evaluation for answer ${it.id}: ${e.message}")
        null
      }
    }
  }

  fun startEvaluation(answer: Answer) {
    containerService.saveAnswerFiles(answer)
    check(!isEvaluationUpToDate(answer.id)) { "Evaluation is up to date." }
    check(!isEvaluationPending(answer.id)) { "Evaluation is already running or queued." }
    evaluationQueue.insert(answer.id)
  }

  fun getLatestEvaluation(answerId: UUID) = evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(answerId)

  fun isEvaluationPending(answerId: UUID) = isEvaluationInQueue(answerId) || evaluationQueue.isRunning(answerId)

  fun isEvaluationInQueue(answerId: UUID) = evaluationQueue.isQueued(answerId)

  fun isEvaluationUpToDate(answerId: UUID): Boolean = getLatestEvaluation(answerId).map {
    it.filesDigest.contentEquals(fileService.getCollectionMd5Digest(answerId))
  }.orElse(false)

  fun getEvaluationRunner(name: String): EvaluationRunner = runnersByName[name]
      ?: throw IllegalArgumentException("Evaluation runner '$name' not found")

  fun getEvaluation(evaluationId: UUID): Evaluation {
    return evaluationRepository.findById(evaluationId).orElseThrow { EntityNotFoundException("Evaluation not found") }
  }
}
