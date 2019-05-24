package de.code_freak.codefreak.service

import com.beust.klaxon.Klaxon
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID
import javax.transaction.Transactional

@Service
class AssignmentService {
  @Autowired
  lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  lateinit var submissionRepository: SubmissionRepository

  @Autowired
  lateinit var answerRepository: AnswerRepository

  @Transactional
  fun findAssignment(id: UUID): Assignment = assignmentRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Assignment not found") }

  @Transactional
  fun findSubmission(id: UUID): Submission = submissionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Submission not found") }

  @Transactional
  fun findSubmissionsOfAssignment(assignmentId: UUID) = submissionRepository.findByAssignmentId(assignmentId)

  fun createNewSubmission(assignment: Assignment): Submission {
    // TODO: attach current user to submission
    val submission = Submission(assignment = assignment)
    submissionRepository.save(submission)

    // create a submission for every task in this assignment
    assignment.tasks.forEach {
      val taskSubmission = Answer(submission, it, it.files?.clone())
      answerRepository.save(taskSubmission)
    }

    return submission
  }

  @Transactional
  fun findAllAssignments(): Iterable<Assignment> = assignmentRepository.findAll()

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
    }

    val archive = TarUtil.createTarFromDirectory(tmpDir)
    tmpDir.deleteRecursively()
    return archive
  }
}
