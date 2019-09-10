package de.code_freak.codefreak.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.io.FileMatchers
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
  lateinit var latexService: LatexService
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

  @Test
  fun createNewSubmission() {
    val out = ByteArrayOutputStream()
    `when`(fileService.readCollectionTar(eq(assignment.id))).thenReturn(files.inputStream())
    `when`(fileService.writeCollectionTar(eq(answer.id))).thenReturn(out)
    `when`(answerRepository.save<Answer>(anyOrNull())).then { it.getArgument(0) }
    val submission = submissionService.createNewSubmission(assignment, user)
    assertThat(submission.answers, hasSize(1))
    assertThat(submission.answers.first(), instanceOf(Answer::class.java))
    verify(answerService, times(1)).copyFilesFromTask(submission.answers.first())
  }

  @Test
  fun createTarArchiveOfSubmissions() {
    val out = ByteArrayOutputStream()
    `when`(assignmentRepository.findById(any())).thenReturn(Optional.of(assignment))
    `when`(submissionRepository.findByAssignmentId(anyOrNull())).thenReturn(listOf(submission))
    `when`(fileService.readCollectionTar(eq(assignment.id))).thenReturn(files.inputStream())
    `when`(latexService.submissionToPdf(anyOrNull(), anyOrNull())).then { }
    submissionService.createTarArchiveOfSubmissions(assignment.id, out)
    val tmpDir = createTempDir()
    TarUtil.extractTarToDirectory(out.toByteArray().inputStream(), tmpDir)
    assertThat(tmpDir.listFiles().first(), FileMatchers.aFileNamed(equalTo(submission.id.toString())))
    tmpDir.deleteRecursively()
  }
}
