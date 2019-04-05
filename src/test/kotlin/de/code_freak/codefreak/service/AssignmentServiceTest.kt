package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.AssignmentTask
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.entity.TaskSubmission
import de.code_freak.codefreak.repository.AssignmentRepository
import de.code_freak.codefreak.repository.SubmissionRepository
import de.code_freak.codefreak.repository.TaskSubmissionRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.sameInstance
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Optional
import java.util.UUID

class AssignmentServiceTest {
  @Mock
  lateinit var assignment: Assignment
  @Mock
  lateinit var assignmentTask: AssignmentTask
  @Mock
  lateinit var assignmentRepository: AssignmentRepository
  @Mock
  lateinit var submission: Submission
  @Mock
  lateinit var submissionRepository: SubmissionRepository
  @Mock
  lateinit var taskSubmissionRepository: TaskSubmissionRepository
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
    val files = ByteArray(0)
    `when`(assignmentTask.files).thenReturn(files)
    `when`(assignment.tasks).thenReturn(mutableListOf(assignmentTask))
    val submission = assignmentService.createNewSubmission(assignment)
    assertThat(submission.taskSubmissions, hasSize(1))
    assertThat(submission.taskSubmissions[0], instanceOf(TaskSubmission::class.java))
    assertThat(submission.taskSubmissions[0].files, instanceOf(ByteArray::class.java))
    assertThat(submission.taskSubmissions[0].files, not(sameInstance(files)))
  }
}
