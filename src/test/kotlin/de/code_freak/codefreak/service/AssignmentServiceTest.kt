package de.code_freak.codefreak.service

import com.nhaarman.mockitokotlin2.anyOrNull
import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.entity.Task
import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.repository.AnswerRepository
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.util.TarUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.sameInstance
import org.hamcrest.io.FileMatchers
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.core.io.ClassPathResource
import java.util.Optional
import java.util.UUID

class AssignmentServiceTest {
  val files = TarUtil.createTarFromDirectory(ClassPathResource("util/tar-sample").file)
  val assignment = Assignment("Assignment 1", User(), null)
  val task = Task(assignment, position = 0L, title = "Task 1", body = "Do stuff", files = files, weight = 100)
  val submission = Submission(null, assignment)
  val answer = Answer(submission, task, files)

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
  @InjectMocks
  val assignmentService = AssignmentService()

  @Before
  fun init() {
    MockitoAnnotations.initMocks(this)
  }

  @Test
  fun `findAssignment by ID`() {
    `when`(assignmentRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(assignment))
    assertThat(assignmentService.findAssignment(UUID(0, 0)), equalTo(assignment))
  }

  @Test(expected = EntityNotFoundException::class)
  fun `findAssignment throws for no results`() {
    `when`(assignmentRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.empty())
    assignmentService.findAssignment(UUID(0, 0))
  }

  @Test
  fun `findSubmission by ID`() {
    `when`(submissionRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(submission))
    assertThat(assignmentService.findSubmission(UUID(0, 0)), equalTo(submission))
  }

  @Test(expected = EntityNotFoundException::class)
  fun `findSubmission throws for no results`() {
    `when`(submissionRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.empty())
    assignmentService.findSubmission(UUID(0, 0))
  }

  @Test
  fun createNewSubmission() {
    val submission = assignmentService.createNewSubmission(assignment)
    assertThat(submission.answers, hasSize(1))
    assertThat(submission.answers.first(), instanceOf(Answer::class.java))
    assertThat(submission.answers.first().files, instanceOf(ByteArray::class.java))
    assertThat(submission.answers.first().files, not(sameInstance(files)))
  }

  @Test
  fun createTarArchiveOfSubmissions() {
    `when`(assignmentRepository.findById(ArgumentMatchers.any())).thenReturn(Optional.of(assignment))
    `when`(submissionRepository.findByAssignmentId(anyOrNull())).thenReturn(listOf(submission))
    val archive = assignmentService.createTarArchiveOfSubmissions(assignment.id)
    val tmpDir = createTempDir()
    TarUtil.extractTarToDirectory(archive, tmpDir)
    assertThat(tmpDir.listFiles().first(), FileMatchers.aFileNamed(equalTo(submission.id.toString())))
    tmpDir.deleteRecursively()
  }
}
