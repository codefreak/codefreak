package de.code_freak.codefreak.service.evaluation

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.entity.EvaluationStep
import de.code_freak.codefreak.entity.Feedback
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.EvaluationRepository
import de.code_freak.codefreak.service.AssignmentStatusChangedEvent
import de.code_freak.codefreak.service.BaseService
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.EntityNotFoundException
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.orNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EvaluationService : BaseService(), ApplicationListener<AssignmentStatusChangedEvent> {

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
  private lateinit var taskService: TaskService

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
    check(!isEvaluationUpToDate(answer)) { "Evaluation is up to date." }
    check(!isEvaluationPending(answer.id)) { "Evaluation is already running or queued." }
    evaluationQueue.insert(answer.id)
  }

  fun getLatestEvaluation(answerId: UUID) = evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(answerId)

  fun isEvaluationPending(answerId: UUID) = isEvaluationInQueue(answerId) || evaluationQueue.isRunning(answerId)

  fun isEvaluationInQueue(answerId: UUID) = evaluationQueue.isQueued(answerId)

  fun getEvaluationByDigest(answerId: UUID, digest: ByteArray): Evaluation? {
    return evaluationRepository.findFirstByAnswerIdAndFilesDigest(answerId, digest).orNull()
  }

  fun createEvaluation(answer: Answer): Evaluation {
    return evaluationRepository.save(Evaluation(answer, fileService.getCollectionMd5Digest(answer.id)))
  }

  fun createCommentFeedback(author: User, comment: String): Feedback {
    // use the first 10 words of the first line or max. 100 chars as summary
    var summary = comment.trim().replace("((?:[^ \\n]+ ?){0,10})".toRegex(), "$1").trim()
    if (summary.length > 100) {
      summary = summary.substring(0..100) + "..."
    }
    return Feedback(summary).apply {
      longDescription = comment
      this.author = author
    }
  }

  fun addCommentFeedback(answer: Answer, digest: ByteArray, feedback: Feedback): Feedback {
    // find out if evaluation has a comment step definition
    val taskDefinition = taskService.getTaskDefinition(answer.task.id)
    val stepDefinition = taskDefinition.evaluation.find { it.step == "comments" }
        ?: throw IllegalArgumentException("Task has no 'comments' evaluation step")
    val stepIndex = taskDefinition.evaluation.indexOf(stepDefinition)
    val evaluation = getEvaluationByDigest(answer.id, digest) ?: createEvaluation(answer)

    // either take existing comments step on evaluation or create a new one
    val evaluationStep = evaluation.evaluationSteps.find { it.position == stepIndex }
        ?: EvaluationStep("comments", stepIndex).also { evaluation.addStep(it) }

    evaluationStep.addFeedback(feedback)
    evaluationRepository.save(evaluation)
    return feedback
  }

  fun isEvaluationUpToDate(answer: Answer): Boolean {
    return getLatestEvaluation(answer.id).map {
      // evaluation is fresh if file hash matches and all evaluation steps have been run
      it.filesDigest.contentEquals(fileService.getCollectionMd5Digest(answer.id)) &&
          taskService.getTaskDefinition(answer.task.id).evaluation.size == it.evaluationSteps.size
    }.orElse(false)
  }

  fun getEvaluationRunner(name: String): EvaluationRunner = runnersByName[name]
      ?: throw IllegalArgumentException("Evaluation runner '$name' not found")

  fun getEvaluation(evaluationId: UUID): Evaluation {
    return evaluationRepository.findById(evaluationId).orElseThrow { EntityNotFoundException("Evaluation not found") }
  }

  override fun onApplicationEvent(event: AssignmentStatusChangedEvent) {
    println("Assignment ${event.assignmentId} status has changed: ${event.status}")
  }
}
