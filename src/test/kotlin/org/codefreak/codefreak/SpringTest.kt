package org.codefreak.codefreak

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.entity.Task
import org.codefreak.codefreak.entity.User
import org.codefreak.codefreak.repository.AnswerRepository
import org.codefreak.codefreak.repository.AssignmentRepository
import org.codefreak.codefreak.repository.SubmissionRepository
import org.codefreak.codefreak.repository.TaskRepository
import org.codefreak.codefreak.repository.UserRepository
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.time.Instant

@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
abstract class SpringTest {
  protected lateinit var assignment: Assignment
  protected lateinit var task: Task
  protected lateinit var submission: Submission
  protected lateinit var answer: Answer
  protected lateinit var user: User

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  @Autowired
  private lateinit var taskRepository: TaskRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var submissionRepository: SubmissionRepository

  @Autowired
  private lateinit var answerRepository: AnswerRepository

  protected fun seedDatabase() {
    user = User("demo")
    userRepository.save(user)
    assignment = Assignment("Test Assignment", user, active = true)
    assignment.openFrom = Instant.now()
    task = Task(assignment, user, 0L, "Demo Task")
    assignment.tasks.add(task)
    assignmentRepository.save(assignment)
    taskRepository.save(task)
    submission = Submission(user, assignment)
    submissionRepository.save(submission)
    answer = Answer(submission, task)
    answerRepository.save(answer)
  }

  protected fun clearDatabase() {
    submissionRepository.delete(submission)
    taskRepository.delete(task)
    assignmentRepository.delete(assignment)
    userRepository.delete(user)
  }
}
