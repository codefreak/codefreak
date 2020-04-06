package org.codefreak.codefreak.service

import org.codefreak.codefreak.Env
import org.codefreak.codefreak.auth.Role
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.repository.UserRepository
import org.codefreak.codefreak.util.TarUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.Ordered
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Seed the database with some initial value
 * This should only be needed until we have a UI for creation
 */
@Service
@Profile(Env.DEV)
class SeedDatabaseService : ApplicationListener<ContextRefreshedEvent>, Ordered {

  @Autowired lateinit var userRepository: UserRepository
  @Autowired lateinit var assignmentRepository: AssignmentRepository
  @Autowired lateinit var taskService: TaskService
  @Autowired lateinit var assignmentService: AssignmentService

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    private const val DEV_USER_PASSWORD = "{noop}123"
    val admin = User("admin").apply {
      roles = mutableSetOf(Role.ADMIN)
      firstName = "John"
      lastName = "Admin"
      password = DEV_USER_PASSWORD
    }
    val teacher = User("teacher").apply {
      roles = mutableSetOf(Role.TEACHER)
      firstName = "Kim"
      lastName = "Teacher"
      password = DEV_USER_PASSWORD
    }
    val student = User("student").apply {
      roles = mutableSetOf(Role.STUDENT)
      firstName = "Alice"
      lastName = "Student"
      password = DEV_USER_PASSWORD
    }
  }

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    if (assignmentRepository.count() > 0) {
      return
    }

    log.info("Initializing database with sample data")

    userRepository.saveAll(listOf(admin, teacher, student))

    val assignment1 = Assignment("C Assignment", teacher, Instant.now().plusSeconds(60), active = true)
    val assignment2 = Assignment("Java Assignment", teacher, Instant.now(), active = true)
    assignmentRepository.saveAll(listOf(assignment1, assignment2))

    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/c-add").file, it)
      taskService.createFromTar(it.toByteArray(), assignment1, teacher, 0)
    }
    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/java-add").file, it)
      taskService.createFromTar(it.toByteArray(), assignment2, teacher, 0)
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

  override fun getOrder(): Int {
    return Ordered.LOWEST_PRECEDENCE
  }
}
