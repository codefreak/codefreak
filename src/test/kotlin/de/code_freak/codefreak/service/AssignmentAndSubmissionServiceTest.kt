package de.code_freak.codefreak.service

import com.nhaarman.mockitokotlin2.any
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.service.file.FileService
import de.code_freak.codefreak.util.TarUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.util.Optional
import java.util.UUID

class AssignmentAndSubmissionServiceTest {
  private val files = ByteArrayOutputStream().use {
    TarUtil.createTarFromDirectory(ClassPathResource("util/tar-sample").file, it); it.toByteArray()
  }
  private val assignment = Assignment("Assignment 1", User("user"), null)
  private val task = Task(assignment, position = 0L, title = "Task 1", body = "Do stuff", weight = 100)
  private val user = User("user")
  private val submission = Submission(user, assignment)
  private val answer = Answer(submission, task)

  init {
    assignment.tasks = sortedSetOf(task)
    submission.answers = mutableSetOf(answer)
  }

  @Mock
  lateinit var assignmentRepository: AssignmentRepository
  @Mock
  lateinit var submissionRepository: SubmissionRepository
  @Mock
  lateinit var answerRepository: AnswerRepository
  @Mock
  lateinit var answerService: AnswerService
  @Mock
  lateinit var fileService: FileService
  @InjectMocks
  val assignmentService = AssignmentService()
  @InjectMocks
  val submissionService = SubmissionService()

  @Before
  fun init() {
    MockitoAnnotations.initMocks(this)
  }

  @Test
  fun `findAssignment by ID`() {
    `when`(assignmentRepository.findById(any())).thenReturn(Optional.of(assignment))
    assertThat(assignmentService.findAssignment(UUID(0, 0)), equalTo(assignment))
  }

  @Test(expected = EntityNotFoundException::class)
  fun `findAssignment throws for no results`() {
    `when`(assignmentRepository.findById(any())).thenReturn(Optional.empty())
    assignmentService.findAssignment(UUID(0, 0))
  }

  @Test
  fun `findSubmission by ID`() {
    `when`(submissionRepository.findById(any())).thenReturn(Optional.of(submission))
    assertThat(submissionService.findSubmission(UUID(0, 0)), equalTo(submission))
  }

  @Test(expected = EntityNotFoundException::class)
  fun `findSubmission throws for no results`() {
    `when`(submissionRepository.findById(any())).thenReturn(Optional.empty())
    submissionService.findSubmission(UUID(0, 0))
  }
}
