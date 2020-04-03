package org.codefreak.codefreak.service.evaluation

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.EvaluationRepository
import org.codefreak.codefreak.service.AssignmentStatusChangedEvent
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.SubmissionService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.evaluation.runner.CommentRunner
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.orNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var runners: List<EvaluationRunner>

  private val runnersByName by lazy { runners.map { it.getName() to it }.toMap() }

  private val log = LoggerFactory.getLogger(this::class.java)

  fun startAssignmentEvaluation(assignmentId: UUID): List<Answer> {
    val submissions = submissionService.findSubmissionsOfAssignment(assignmentId)
    return submissions.flatMap { it.answers }.mapNotNull {
      try {
        // this will never be run by a student so force file saving should be safe
        startEvaluation(it, forceSaveFiles = true)
        it
      } catch (e: IllegalStateException) {
        // evaluation is already fresh or running
        log.debug("Not queuing evaluation for answer ${it.id}: ${e.message}")
        null
      }
    }
  }

  fun startEvaluation(answer: Answer, forceSaveFiles: Boolean = false) {
    containerService.saveAnswerFiles(answer, forceSaveFiles)
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
    val stepDefinition = taskDefinition.evaluation.find { it.step == CommentRunner.RUNNER_NAME }
        ?: throw IllegalArgumentException("Task has no 'comments' evaluation step")
    val stepIndex = taskDefinition.evaluation.indexOf(stepDefinition)
    val evaluation = getEvaluationByDigest(answer.id, digest) ?: createEvaluation(answer)

    // either take existing comments step on evaluation or create a new one
    val evaluationStep = evaluation.evaluationSteps.find { it.position == stepIndex }
        ?: EvaluationStep(CommentRunner.RUNNER_NAME, stepIndex).also { evaluation.addStep(it) }

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

  @EventListener
  @Transactional
  fun onApplicationEvent(event: AssignmentStatusChangedEvent) {
    if (event.status != AssignmentStatus.CLOSED) {
      return
    }
    log.info("Automatically trigger evaluation for answers of ${event.assignmentId}")
    startAssignmentEvaluation(event.assignmentId)
  }
}
