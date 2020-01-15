package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.service.evaluation.EvaluationService
import de.code_freak.codefreak.service.file.FileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import java.util.UUID

@Service
class SubmissionService : BaseService() {

  @Autowired
  lateinit var submissionRepository: SubmissionRepository

  @Autowired
  lateinit var fileService: FileService

  @Autowired
  private lateinit var taskService: TaskService

  @Autowired
  private lateinit var evaluationService: EvaluationService

  @Autowired
  private lateinit var spreadsheetService: SpreadsheetService

  @Autowired
  private lateinit var assignmentService: AssignmentService

  @Transactional
  fun findSubmission(id: UUID): Submission = submissionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Submission not found") }

  @Transactional
  fun findSubmission(assignmentId: UUID, userId: UUID): Optional<Submission> =
      submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)

  @Transactional
  fun findOrCreateSubmission(assignmentId: UUID, user: User): Submission =
      submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.id)
          .orElseGet { createSubmission(assignmentService.findAssignment(assignmentId), user) }

  @Transactional
  fun findSubmissionsOfAssignment(assignmentId: UUID) = submissionRepository.findByAssignmentId(assignmentId)

  @Transactional
  fun findSubmissionsOfUser(userId: UUID) = submissionRepository.findAllByUserId(userId)

  @Transactional
  fun createSubmission(assignment: Assignment, user: User): Submission {
    val submission = Submission(assignment = assignment, user = user)
    return submissionRepository.save(submission)
  }

  fun generateSubmissionCsv(assignment: Assignment): String {
    // store a list of (task -> evaluation steps) that represents each column in our table
    val columnDefinitions = assignment.tasks.flatMap { task ->
      taskService.getTaskDefinition(task.id).evaluation.mapIndexed { index, evaluationDefinition ->
        Triple(task, index, evaluationDefinition)
      }
    }
    // generate the header columns. In CSV files we have no option to join columns so we have to create a flat
    // list of task-evaluation combinations
    // [EMPTY] | Task #1 Eval #1 | Task #1 Eval #2 | Task #2 Eval #1 | ...
    val resultTitles = columnDefinitions.map { (task, _, evaluationDefinition) ->
      "${task.title} (${evaluationDefinition.step})"
    }
    val titleCols = mutableListOf("User").apply {
      addAll(resultTitles)
    }
    val submissions = findSubmissionsOfAssignment(assignment.id)

    // generate the actual data rows for each submission
    val rows = submissions.map { submission ->
      val resultCols = columnDefinitions.map { (task, index, _) ->
        val answer = submission.getAnswer(task.id)
        val evaluation = answer?.id?.let { evaluationService.getLatestEvaluation(it).orElse(null) }
        val result = evaluation?.evaluationSteps?.get(index)?.summary
        when {
          answer == null -> "[no answer]"
          evaluation == null -> "[no evaluation]"
          else -> result ?: "[no result]"
        }
      }

      // all columns combined starting with the username
      mutableListOf(submission.user.username).apply {
        addAll(resultCols)
      }
    }

    return spreadsheetService.generateCsv(titleCols, rows)
  }
}
