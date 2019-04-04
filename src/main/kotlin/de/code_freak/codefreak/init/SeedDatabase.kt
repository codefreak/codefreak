package de.code_freak.codefreak.init

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.AssignmentTask
import de.code_freak.codefreak.entity.Classroom
import de.code_freak.codefreak.entity.TaskEvaluation
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.AssignmentTaskRepository
import de.code_freak.codefreak.repository.ClassroomRepository
import de.code_freak.codefreak.repository.TaskEvaluationRepository
import de.code_freak.codefreak.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.Ordered
import org.springframework.stereotype.Service

/**
 * Seed the database with some initial value
 * This should only be needed until we have a UI for creation
 */
@Service
@Profile("dev")
class SeedDatabase : ApplicationListener<ContextRefreshedEvent>, Ordered {

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  lateinit var assignmentTaskRepository: AssignmentTaskRepository

  @Autowired
  lateinit var classroomRepository: ClassroomRepository

  @Autowired
  lateinit var taskEvaluationRepository: TaskEvaluationRepository

  @Value("\${spring.jpa.hibernate.ddl-auto:''}")
  private lateinit var schemaExport: String

  @Value("\${spring.jpa.database:''}")
  private lateinit var database: String

  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    if (!schemaExport.startsWith("create") && database != "HSQL") {
      return
    }

    val user1 = User()
    val user2 = User()
    userRepository.saveAll(listOf(user1, user2))

    val classroom1 = Classroom("Classroom 1")
    val classroom2 = Classroom("Classroom 2")
    classroomRepository.saveAll(listOf(classroom1, classroom2))

    val assignment1 = Assignment("C Assignment", user1, classroom1)
    val assignment2 = Assignment("Java Assignment", user2, classroom2)
    assignmentRepository.saveAll(listOf(assignment1, assignment2))

    val task1 = AssignmentTask(assignment1, 0, "Program in C", "Write a function `add(int a, int b)` that returns the sum of `a` and `b`", this::class.java.classLoader.getResource("init/c-task.tar").readBytes(), 100)
    val task2 = AssignmentTask(assignment2, 0, "Program in Java", "Write a function `add(int a, int b)` that returns the sum of `a` and `b`", this::class.java.classLoader.getResource("init/java-task.tar").readBytes(), 100)
    assignmentTaskRepository.saveAll(listOf(task1, task2))

    val eval1 = TaskEvaluation(task1, "exec", hashMapOf("CMD" to "gcc -o main && ./main"))
    val eval2 = TaskEvaluation(task2, "exec", hashMapOf("CMD" to "javac Main.java && java Main"))
    taskEvaluationRepository.saveAll(listOf(eval1, eval2))
  }

  override fun getOrder(): Int {
    return Ordered.LOWEST_PRECEDENCE
  }
}
