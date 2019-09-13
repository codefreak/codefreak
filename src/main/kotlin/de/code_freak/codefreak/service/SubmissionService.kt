package de.code_freak.codefreak.service

import com.beust.klaxon.Klaxon
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import de.code_freak.codefreak.util.afterClose
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.OutputStream
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

  @Autowired
  lateinit var fileService: FileService

  @Autowired
  lateinit var answerService: AnswerService

  @Transactional
  fun findSubmission(id: UUID): Submission = submissionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Submission not found") }

  @Transactional
  fun findSubmission(assignmentId: UUID, userId: UUID): Optional<Submission> =
      submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)

  @Transactional
  fun findSubmissionsOfAssignment(assignmentId: UUID) = submissionRepository.findByAssignmentId(assignmentId)

  @Transactional
  fun findSubmissionsOfUser(userId: UUID) = submissionRepository.findAllByUserId(userId)

  @Transactional
  fun createNewSubmission(assignment: Assignment, user: User): Submission {
    val submission = Submission(assignment = assignment, user = user)
    submissionRepository.save(submission)

    // create an answer for every task in this assignment
    assignment.tasks.forEach { task ->
      val answer = answerRepository.save(Answer(submission, task))
      answerService.copyFilesFromTask(answer)
    }

    return submission
  }

  fun createTarArchiveOfSubmissions(assignmentId: UUID, out: OutputStream) {
    val submissions = findSubmissionsOfAssignment(assignmentId)
    val tmpDir = createTempDir()
    // extract all submissions and answers into a temporary directory
    submissions.forEach { submission ->
      val submissionDir = File(tmpDir, submission.id.toString())
      submissionDir.mkdirs()
      submission.answers.forEach { answer ->
        val answerDir = File(submissionDir, answer.id.toString())
        if (fileService.collectionExists(answer.id)) {
          fileService.readCollectionTar(answer.id).use { tar ->
            TarUtil.extractTarToDirectory(tar, answerDir)
          }
        } else {
          answerDir.mkdirs()
        }
      }
      // write a meta-file with information about user
      val metaFile = File(submissionDir, "freak.json")
      metaFile.writeText(Klaxon().toJsonString(submission.user))
      // write pdf with submission
      val pdfFile = File(submissionDir, "submission.pdf")
      pdfFile.outputStream().use { latexService.submissionToPdf(submission, it) }
    }

    TarUtil.createTarFromDirectory(tmpDir, out.afterClose { tmpDir.deleteRecursively() })
  }
}
