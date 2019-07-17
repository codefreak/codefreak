package de.code_freak.codefreak.service

import com.beust.klaxon.Klaxon
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.Optional
import java.util.UUID
import javax.transaction.Transactional

@Service
class SubmissionService : BaseService() {

  @Autowired
  lateinit var submissionRepository: SubmissionRepository

  @Autowired
  lateinit var answerRepository: AnswerRepository

  @Autowired
  lateinit var latexService: LatexService

  @Transactional
  fun findSubmission(id: UUID): Submission = submissionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Submission not found") }

  @Transactional
  fun findSubmission(assignmentId: UUID, userId: UUID): Optional<Submission> =
      submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)

  @Transactional
  fun findSubmissionsOfAssignment(assignmentId: UUID) = submissionRepository.findByAssignmentId(assignmentId)

  @Transactional
  fun createNewSubmission(assignment: Assignment, user: User): Submission {
    val submission = Submission(assignment = assignment, user = user)
    submissionRepository.save(submission)

    // create a submission for every task in this assignment
    assignment.tasks.forEach {
      val taskSubmission = Answer(submission, it, it.files?.clone())
      answerRepository.save(taskSubmission)
    }

    return submission
  }

  fun createTarArchiveOfSubmissions(assignmentId: UUID): ByteArray {
    val submissions = findSubmissionsOfAssignment(assignmentId)
    val tmpDir = createTempDir()
    // extract all submissions and answers into a temporary directory
    submissions.forEach {
      val submissionDir = File(tmpDir, it.id.toString())
      submissionDir.mkdirs()
      it.answers.forEach {
        val answerDir = File(submissionDir, it.id.toString())
        val files = it.files
        if (files != null) {
          TarUtil.extractTarToDirectory(files, answerDir)
        } else {
          answerDir.mkdirs()
        }
      }
      // write a meta-file with information about user
      val metaFile = File(submissionDir, "freak.json")
      metaFile.writeText(Klaxon().toJsonString(it.user))
      // write pdf with submission
      val pdfFile = File(submissionDir, "submission.pdf")
      pdfFile.writeBytes(latexService.submissionToPdf(it))
    }

    val archive = TarUtil.createTarFromDirectory(tmpDir)
    tmpDir.deleteRecursively()
    return archive
  }
}
