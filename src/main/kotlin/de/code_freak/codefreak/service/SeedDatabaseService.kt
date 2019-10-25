package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Classroom
import de.code_freak.codefreak.entity.Requirement
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.ClassroomRepository
import de.code_freak.codefreak.repository.RequirementRepository
import de.code_freak.codefreak.repository.UserRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
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
@Profile("dev")
class SeedDatabaseService : ApplicationListener<ContextRefreshedEvent>, Ordered {

  @Autowired lateinit var userRepository: UserRepository
  @Autowired lateinit var assignmentRepository: AssignmentRepository
  @Autowired lateinit var classroomRepository: ClassroomRepository
  @Autowired lateinit var requirementRepository: RequirementRepository
  @Autowired lateinit var fileService: FileService
  @Autowired lateinit var taskService: TaskService
  @Autowired lateinit var assignmentService: AssignmentService

  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    val admin = User("admin", "John", "Admin")
    val teacher = User("teacher", "Kim", "Teacher")
    val student = User("student", "Alice", "Student")
  }

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    if (assignmentRepository.count() > 0) {
      return
    }

    log.info("Initializing database with sample data")

    userRepository.saveAll(listOf(admin, teacher, student))

    val classroom1 = Classroom("Classroom 1")
    val classroom2 = Classroom("Classroom 2")
    classroomRepository.saveAll(listOf(classroom1, classroom2))

    val assignment1 = Assignment("C Assignment", teacher, classroom1)
    val assignment2 = Assignment("Java Assignment", teacher, classroom2)
    assignmentRepository.saveAll(listOf(assignment1, assignment2))

    val task1 = ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/c-add").file, it)
      taskService.createFromTar(it.toByteArray(), assignment1, 0)
    }
    val task2 = ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks/java-add").file, it)
      taskService.createFromTar(it.toByteArray(), assignment2, 0)
    }

    val eval1 = Requirement(task1, "exec", hashMapOf("CMD" to "gcc -o main && ./main"))
    val eval2 = Requirement(task2, "exec", hashMapOf("CMD" to "javac Main.java && java Main"))
    requirementRepository.saveAll(listOf(eval1, eval2))

    ByteArrayOutputStream().use {
      TarUtil.createTarFromDirectory(ClassPathResource("init/tasks").file, it)
      assignmentService.createFromTar(it.toByteArray(), teacher, Instant.now().plusMillis(60*1000)).let { result ->
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
