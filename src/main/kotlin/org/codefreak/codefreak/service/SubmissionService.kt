package org.codefreak.codefreak.service

import java.io.OutputStream
import java.util.Optional
import java.util.UUID
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.SubmissionRepository
import org.codefreak.codefreak.service.evaluation.EvaluationService
import org.codefreak.codefreak.service.file.FileService
import org.codefreak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubmissionService : BaseService() {

  @Autowired
  lateinit var submissionRepository: SubmissionRepository

  @Autowired
  lateinit var fileService: FileService

  @Autowired
  private lateinit var evaluationService: EvaluationService

  @Autowired
  private lateinit var spreadsheetService: SpreadsheetService

  @Autowired
  private lateinit var assignmentService: AssignmentService

  @Autowired
  private lateinit var answerService: AnswerService

  @Transactional
  fun findSubmission(id: UUID): Submission = submissionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Submission not found") }

  @Transactional
  fun findSubmission(assignmentId: UUID, userId: UUID): Optional<Submission> =
      submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)

  @Transactional
  fun deleteSubmission(submissionId: UUID) {
    // call respective delete method for each answer
    findSubmission(submissionId).answers.forEach { answerService.deleteAnswer(it.id) }
    submissionRepository.deleteById(submissionId)
  }

  @Transactional
  fun findOrCreateSubmission(assignmentId: UUID?, user: User): Submission =
      submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.id)
          .orElseGet { createSubmission(assignmentId?.let { assignmentService.findAssignment(it) }, user) }

  @Transactional
  fun findSubmissionsOfAssignment(assignmentId: UUID) = submissionRepository.findByAssignmentId(assignmentId)

  @Transactional
  fun findSubmissionsOfUser(userId: UUID) = submissionRepository.findAllByUserId(userId)

  @Transactional
  fun createSubmission(assignment: Assignment?, user: User): Submission {
    val submission = Submission(assignment = assignment, user = user)
    return submissionRepository.save(submission)
  }

  @Transactional(readOnly = true)
  fun generateSubmissionCsv(assignmentId: UUID): String {
    val assignment = assignmentService.findAssignment(assignmentId)
    // store a list of (task -> evaluation steps) that represents each column in our table
    val columnDefinitions = assignment.tasks.flatMap { task -> task.evaluationStepDefinitions.map { Pair(task, it) } }
    // generate the header columns. In CSV files we have no option to join columns so we have to create a flat
    // list of task-evaluation combinations
    // [EMPTY] | Task #1 Eval #1 | Task #1 Eval #2 | Task #2 Eval #1 | ...
    val resultTitles = columnDefinitions.map { (task, evaluationStepDefinition) ->
      "${task.title} (${evaluationStepDefinition.runnerName})"
    }
    val titleCols = mutableListOf("User").apply {
      addAll(resultTitles)
    }
    val submissions = findSubmissionsOfAssignment(assignment.id)

    // generate the actual data rows for each submission
    val rows = submissions.map { submission ->
      val resultCols = columnDefinitions.map { (task, evaluationStepDefinition) ->
        val answer = submission.getAnswer(task.id)
        val evaluation = answer?.id?.let { evaluationService.getLatestEvaluation(it).orElse(null) }
        val result = evaluation?.evaluationSteps?.firstOrNull { it.definition == evaluationStepDefinition }?.summary
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

  @Transactional(readOnly = true)
  fun generateSubmissionsTar(assignmentId: UUID, outputStream: OutputStream) {
    val assignment = assignmentService.findAssignment(assignmentId)
    val outputArchive = TarUtil.PosixTarArchiveOutputStream(outputStream)
    assignment.submissions.forEach { submission ->
      val submissionPath = "/${submission.user.username}"
      TarUtil.mkdir(submissionPath, outputArchive)
      submission.answers.forEach { answer ->
        val answerPath = "$submissionPath/task-${answer.task.position}"
        TarUtil.mkdir(answerPath, outputArchive)
        fileService.readCollectionTar(answer.id).use { answerFiles ->
          val source = TarArchiveInputStream(answerFiles)
          TarUtil.copyEntries(source, outputArchive, prefix = answerPath, filter = {
            // don't copy root directory of answer as we already created one
            it.name.trimStart('/', '.').isNotEmpty()
          })
        }
      }
    }
    outputArchive.finish()
  }
}
