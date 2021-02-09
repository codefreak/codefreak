package org.codefreak.codefreak.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.UUID
import liquibase.util.StreamUtil
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.util.TarUtil
import org.codefreak.codefreak.util.TarUtil.getCodefreakDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssignmentService : BaseService() {

  data class AssignmentCreationResult(val assignment: Assignment, val taskErrors: Map<String, Throwable>)

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  private lateinit var submissionService: SubmissionService

  @Autowired
  private lateinit var taskTarService: TaskTarService

  @Autowired
  private lateinit var self: AssignmentService

  @Autowired
  @Qualifier("yamlObjectMapper")
  private lateinit var yamlMapper: ObjectMapper

  @Transactional
  fun findAssignment(id: UUID): Assignment = assignmentRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Assignment not found") }

  fun findAssignmentById(id: UUID) : Assignment = assignmentRepository.findById(id).get()

  @Transactional
  fun findAllAssignments(): Iterable<Assignment> = assignmentRepository.findAll()

  @Transactional
  fun findAssignmentsByOwner(owner: User): Iterable<Assignment> = assignmentRepository.findByOwnerId(owner.id)

  @Transactional
  fun findAllAssignmentsForUser(userId: UUID) = submissionService.findSubmissionsOfUser(userId)
      .filter { it.assignment != null }
      .map { it.assignment!! }
      .filter { it.active }

  @Transactional
  fun createFromTar(content: ByteArray, owner: User, modify: Assignment.() -> Unit = {}): AssignmentCreationResult {
    val definition = yamlMapper.getCodefreakDefinition<AssignmentDefinition>(ByteArrayInputStream(content))
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
          taskTarService.createFromTar(taskContent.toByteArray(), owner, assignment, index.toLong()).let {
            assignment.tasks.add(it)
          }
        }
      } catch (e: Exception) {
        taskErrors[it] = e
      }
    }
    return AssignmentCreationResult(assignment, taskErrors)
  }

  @Transactional
  fun createEmptyAssignment(owner: User): Assignment {
    return ByteArrayOutputStream().use {
      StreamUtil.copy(ClassPathResource("empty_assignment.tar").inputStream, it)
      createFromTar(it.toByteArray(), owner).assignment
    }
  }

  @Transactional
  fun deleteAssignment(id: UUID) {
    // call respective delete method for each submission
    findAssignment(id).submissions.forEach { submissionService.deleteSubmission(it.id) }
    assignmentRepository.deleteById(id)
  }

  @Transactional
  fun addTasksToAssignment(assignment: Assignment, tasks: Collection<Task>) {
    var nextPosition = assignment.tasks.maxByOrNull { it.position }?.let { it.position + 1 } ?: 0
    for (task in tasks) {
      taskTarService.createFromTar(taskTarService.getExportTar(task), assignment.owner, assignment, nextPosition)
      nextPosition++
    }
  }

  @Transactional
  fun saveAssignment(assignment: Assignment) = assignmentRepository.save(assignment)

  @Transactional
  fun getExportTar(assignmentId: UUID): ByteArray {
    val out = ByteArrayOutputStream()
    val tar = TarUtil.PosixTarArchiveOutputStream(out)
    val assignment = findAssignment(assignmentId)

    assignment.tasks.forEach {
      val taskTar = TarArchiveInputStream(ByteArrayInputStream(taskTarService.getExportTar(it)))
      TarUtil.copyEntries(taskTar, tar, prefix = "task-${it.position}/")
    }

    val definition = AssignmentDefinition(
        assignment.title,
        assignment.tasks.map { "task-${it.position}" }
    ).let { yamlMapper.writeValueAsBytes(it) }

    tar.putArchiveEntry(TarArchiveEntry("codefreak.yml").also { it.size = definition.size.toLong() })
    tar.write(definition)
    tar.closeArchiveEntry()
    tar.close()
    return out.toByteArray()
  }
}
