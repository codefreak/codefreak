package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.UUID

@Service
class AssignmentService : BaseService() {

  data class AssignmentCreationResult(val assignment: Assignment, val taskErrors: Map<String, Throwable>)

  @Autowired
  lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  lateinit var submissionService: SubmissionService

  @Autowired
  lateinit var taskService: TaskService

  @Autowired
  lateinit var self: AssignmentService

  @Transactional
  fun findAssignment(id: UUID): Assignment = assignmentRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Assignment not found") }

  @Transactional
  fun findAllAssignments(): Iterable<Assignment> = assignmentRepository.findAll()

  @Transactional
  fun findAllAssignmentsForUser(userId: UUID) = submissionService.findSubmissionsOfUser(userId).map {
    it.assignment
  }

  @Transactional
  fun createFromTar(content: ByteArray, owner: User, modify: Assignment.() -> Unit = {}): AssignmentCreationResult {
    val definition = TarUtil.getYamlDefinition<AssignmentDefinition>(ByteArrayInputStream(content))
    val assignment = self.withNewTransaction {
      val assignment = Assignment(definition.title, owner)
      assignment.modify()
      assignmentRepository.save(assignment)
    }
    val taskErrors = mutableMapOf<String, Throwable>()
    definition.tasks.forEachIndexed { index, it ->
      val taskContent = ByteArrayOutputStream()
      TarUtil.extractSubdirectory(ByteArrayInputStream(content), taskContent, it)
      try {
        self.withNewTransaction {
          taskService.createFromTar(taskContent.toByteArray(), assignment, owner, index.toLong()).let {
            assignment.tasks.add(it)
          }
        }
      } catch (e: Exception) {
        taskErrors[it] = e
      }
    }
    return AssignmentCreationResult(assignment, taskErrors)
  }
}
