package org.codefreak.codefreak.service.evaluation

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.time.Instant
import java.util.UUID
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepDefinition
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.EvaluationRepository
import org.codefreak.codefreak.repository.EvaluationStepDefinitionRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.service.AnswerDeadlineReachedEvent
import org.codefreak.codefreak.service.AnswerService
import org.codefreak.codefreak.service.AssignmentStatusChangedEvent
import org.codefreak.codefreak.service.BaseService
import org.codefreak.codefreak.service.EntityNotFoundException
import org.codefreak.codefreak.service.IdeService
import org.codefreak.codefreak.service.SubmissionService
import org.codefreak.codefreak.service.TaskService
import org.codefreak.codefreak.service.evaluation.runner.CommentRunner
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.PositionUtil
import org.codefreak.codefreak.util.orNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EvaluationService : BaseService() {
  @Autowired
  private lateinit var submissionService: SubmissionService

  @Autowired
  private lateinit var evaluationRepository: EvaluationRepository

  @Autowired
  private lateinit var ideService: IdeService

  @Autowired
  private lateinit var fileService: FileService

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var answerService: AnswerService

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var evaluationQueue: EvaluationQueue

  @Autowired
  private lateinit var runners: List<EvaluationRunner>

  @Autowired
  private lateinit var evaluationStepDefinitionRepository: EvaluationStepDefinitionRepository

  private val runnersByName by lazy { runners.map { it.getName() to it }.toMap() }

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    private val objectMapper = ObjectMapper()
  }

  @Transactional
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

  @Synchronized
  fun startEvaluation(answer: Answer, forceSaveFiles: Boolean = false) {
    ideService.saveAnswerFiles(answer, forceSaveFiles)
    check(!isEvaluationUpToDate(answer)) { "Evaluation is up to date." }
    check(!isEvaluationPending(answer.id)) { "Evaluation is already running or queued." }
    evaluationQueue.insert(answer.id)
  }

  fun getLatestEvaluation(answerId: UUID) = evaluationRepository.findFirstByAnswerIdOrderByCreatedAtDesc(answerId)

  fun isEvaluationPending(answerId: UUID) = isEvaluationInQueue(answerId) || evaluationQueue.isRunning(answerId)

  fun isEvaluationInQueue(answerId: UUID) = evaluationQueue.isQueued(answerId)

  fun getOrCreateValidEvaluationByDigest(answer: Answer, digest: ByteArray): Evaluation {
    return evaluationRepository.findFirstByAnswerIdAndFilesDigestOrderByCreatedAtDesc(answer.id, digest)
        .map { if (it.evaluationSettingsFrom != answer.task.evaluationSettingsChangedAt) createEvaluation(answer) else it }
        .orElseGet { createEvaluation(answer) }
  }

  @Transactional
  fun invalidateEvaluations(task: Task) {
    if (log.isDebugEnabled) {
      log.debug("Invalidating evaluations of task '${task.title}' (taskId=${task.id})")
    }
    task.evaluationSettingsChangedAt = Instant.now()
  }

  @Transactional
  fun invalidateEvaluations(assignment: Assignment) {
    assignment.tasks.forEach(this::invalidateEvaluations)
  }

  fun createEvaluation(answer: Answer): Evaluation {
    return evaluationRepository.save(Evaluation(answer, fileService.getCollectionMd5Digest(answer.id), answer.task.evaluationSettingsChangedAt))
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
    val stepDefinition = answer.task.evaluationStepDefinitions.find { it.runnerName == CommentRunner.RUNNER_NAME }
        ?: throw IllegalArgumentException("Task has no 'comments' evaluation step")
    val evaluation = getOrCreateValidEvaluationByDigest(answer, digest)

    // either take existing comments step on evaluation or create a new one
    val evaluationStep = evaluation.evaluationSteps.find { it.definition == stepDefinition }
        ?: EvaluationStep(stepDefinition).also { evaluation.addStep(it) }

    evaluationStep.addFeedback(feedback)
    evaluationRepository.save(evaluation)
    return feedback
  }

  fun isEvaluationUpToDate(answer: Answer): Boolean {
    // check if any evaluation has been run at all
    val evaluation = getLatestEvaluation(answer.id).orNull() ?: return false
    // check if evaluation settings have been changed since the evaluation was run
    if (evaluation.evaluationSettingsFrom != answer.task.evaluationSettingsChangedAt) {
      return false
    }
    // check if evaluation has been run for latest file state
    if (!evaluation.filesDigest.contentEquals(fileService.getCollectionMd5Digest(answer.id))) {
      return false
    }
    // check if all evaluation steps have been run
    if (answer.task.evaluationStepDefinitions.size != evaluation.evaluationSteps.size) {
      return false
    }
    // allow to re-run if any of the steps errored
    if (evaluation.stepsResultSummary === EvaluationStep.EvaluationStepResult.ERRORED) {
      return false
    }
    return true
  }

  fun getEvaluationRunner(name: String): EvaluationRunner = runnersByName[name]
      ?: throw IllegalArgumentException("Evaluation runner '$name' not found")

  fun getAllEvaluationRunners() = runners

  fun getEvaluation(evaluationId: UUID): Evaluation {
    return evaluationRepository.findById(evaluationId).orElseThrow { EntityNotFoundException("Evaluation not found") }
  }

  @EventListener
  @Transactional
  fun onAnswerDeadlineReached(event: AnswerDeadlineReachedEvent) {
    log.info("Automatically trigger evaluation for answer ${event.answerId}")
    try {
      startEvaluation(answerService.findAnswer(event.answerId), forceSaveFiles = true)
    } catch (e: IllegalStateException) {
      // evaluation has already been triggered
    }
  }

  @EventListener
  @Transactional
  fun onAssignmentClosed(event: AssignmentStatusChangedEvent) {
    if (event.status != AssignmentStatus.CLOSED) {
      return
    }
    log.info("Automatically trigger evaluation for answers of ${event.assignmentId}")
    startAssignmentEvaluation(event.assignmentId)
  }

  fun findEvaluationStepDefinition(id: UUID): EvaluationStepDefinition = evaluationStepDefinitionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Evaluation step definition not found") }

  fun saveEvaluationStepDefinition(definition: EvaluationStepDefinition) = evaluationStepDefinitionRepository.save(definition)

  @Transactional
  fun setEvaluationStepDefinitionPosition(evaluationStepDefinition: EvaluationStepDefinition, newPosition: Long) {
    val task = evaluationStepDefinition.task

    PositionUtil.move(task.evaluationStepDefinitions, evaluationStepDefinition.position.toLong(), newPosition, { position.toLong() }, { position = it.toInt() })

    evaluationStepDefinitionRepository.saveAll(task.evaluationStepDefinitions)
    taskRepository.save(task)
  }

  @Transactional
  fun deleteEvaluationStepDefinition(evaluationStepDefinition: EvaluationStepDefinition) {
    evaluationStepDefinition.task.run {
      evaluationStepDefinitions.filter { it.position > evaluationStepDefinition.position }.forEach { it.position-- }
      evaluationStepDefinitionRepository.saveAll(evaluationStepDefinitions)
    }
    evaluationStepDefinitionRepository.delete(evaluationStepDefinition)
  }

  fun validateRunnerOptions(definition: EvaluationStepDefinition) {
    val runner = getEvaluationRunner(definition.runnerName)
    val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6).getSchema(runner.getOptionsSchema())
    val errors = schema.validate(objectMapper.valueToTree(definition.options))
    require(errors.isEmpty()) { "Runner options for ${definition.runnerName} are invalid: \n" + errors.joinToString("\n") { it.message } }
  }

  @Transactional
  fun updateEvaluationStepDefinition(evaluationStepDefinition: EvaluationStepDefinition, title: String?, active: Boolean?, options: Map<String, Any>?): EvaluationStepDefinition {
    title?.let {
      evaluationStepDefinition.title = it
    }
    active?.let {
      evaluationStepDefinition.active = it
    }
    options?.let {
      evaluationStepDefinition.options = it
    }
    validateRunnerOptions(evaluationStepDefinition)
    saveEvaluationStepDefinition(evaluationStepDefinition)
    taskService.invalidateLatestEvaluations(evaluationStepDefinition.task)

    return evaluationStepDefinition
  }
}
