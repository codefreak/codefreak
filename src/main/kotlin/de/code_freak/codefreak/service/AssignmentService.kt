package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.repository.AnswerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
}
