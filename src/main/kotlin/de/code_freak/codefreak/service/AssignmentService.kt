package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.util.TarUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception
import java.util.UUID
import javax.transaction.Transactional

@Service
class AssignmentService : BaseService() {

  data class AssignmentCreationResult(val assignment: Assignment, val taskErrors: Map<String, Throwable>)

  @Autowired
  lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  lateinit var submissionService: SubmissionService

  @Autowired
  lateinit var taskService: TaskService

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
  fun createFromTar(`in`: InputStream, owner: User): AssignmentCreationResult {
    val content = `in`.readBytes()
    val definition = TarUtil.getYamlDefinition<AssignmentDefinition>(ByteArrayInputStream(content))
    val assignment = assignmentRepository.save(Assignment(definition.title, owner, null))
    val taskErrors = mutableMapOf<String, Throwable>()
    definition.tasks.forEachIndexed { index, it ->
      val taskContent = ByteArrayOutputStream()
      TarUtil.extractSubdirectory(ByteArrayInputStream(content), taskContent, it)
      try {
        taskService.createFromTar(taskContent.toByteArray(), assignment, index.toLong()).let {
          assignment.tasks.add(it)
        }
      } catch (e: Exception) {
        taskErrors[it] = e
      }
    }
    return AssignmentCreationResult(assignment, taskErrors)
  }
}
