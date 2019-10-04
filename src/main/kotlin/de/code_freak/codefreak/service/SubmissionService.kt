package de.code_freak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
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
  lateinit var fileService: FileService

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
  fun createSubmission(assignment: Assignment, user: User): Submission {
    val submission = Submission(assignment = assignment, user = user)
    return submissionRepository.save(submission)
  }
}
