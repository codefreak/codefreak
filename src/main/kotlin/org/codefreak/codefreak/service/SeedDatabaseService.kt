package org.codefreak.codefreak.service

import java.io.ByteArrayOutputStream
import java.time.Instant
import org.codefreak.codefreak.Env
import org.codefreak.codefreak.auth.Role
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.repository.UserRepository
import org.codefreak.codefreak.util.TarUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

/**
 * Seed the database with some initial assignments
 * This should only be needed until we have a UI for creation
 */
@Service
@Profile(Env.DEV)
@Order(Ordered.LOWEST_PRECEDENCE)
class SeedDatabaseService : ApplicationListener<ContextRefreshedEvent> {

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  lateinit var taskService: TaskService

  @Autowired
  lateinit var assignmentService: AssignmentService

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    if (assignmentRepository.count() > 0) {
      return
    }

    // find someone in the DB who is teacher
    val teacher = userRepository.findAll().firstOrNull { it.roles.contains(Role.TEACHER) }

    if (teacher === null) {
      log.warn("Skip seeding of sample data: There are no teachers in your database.")
      return
    }

    log.info("Seeding database with sample data. Assignments belong to user '${teacher.username}'.")

    val assignment1 = Assignment("C Assignment", teacher, Instant.now().plusSeconds(60), active = true)
    val assignment2 = Assignment("Java Assignment", teacher, Instant.now(), active = true)
    assignmentRepository.saveAll(listOf(assignment1, assignment2))

    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/c-add").file, it)
      taskService.createFromTar(it.toByteArray(), assignment1, teacher, 0)
    }
    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/java-add").file, it)
      taskService.createFromTar(it.toByteArray(), assignment2, teacher, 0).also { task ->
        // set a 1h 1min time limit
        task.timeLimit = 3600 + 60
        taskService.saveTask(task)
      }
    }

    // task pool
    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/java-add").file, it)
      taskService.createFromTar(it.toByteArray(), null, teacher, 0)
    }
    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/c-add").file, it)
      taskService.createFromTar(it.toByteArray(), null, teacher, 0)
    }

    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks").file, it)
      assignmentService.createFromTar(it.toByteArray(), teacher) {
        openFrom = Instant.now()
        deadline = Instant.now().plusSeconds(60)
        active = true
      }.let { result ->
        if (result.taskErrors.isNotEmpty()) {
          throw result.taskErrors.values.first()
        }
      }
    }
  }
}
