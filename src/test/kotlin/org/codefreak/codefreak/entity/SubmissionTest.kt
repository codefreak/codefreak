package org.codefreak.codefreak.entity

import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SubmissionTest {
  @Mock
  lateinit var user: User

  @Mock
  lateinit var assignment: Assignment

  @InjectMocks
  lateinit var submission: Submission

  @BeforeEach
  fun init() {
    whenever(assignment.deadline).thenReturn(null)
    whenever(assignment.timeLimit).thenReturn(null)
  }

  @Test
  fun `no deadline if neither assignment has deadline nor submission has time limit`() {
    assertNull(submission.deadline)
  }

  @Test
  fun `use assignment deadline if no time limit is set`() {
    val deadline = Instant.now()
    whenever(assignment.deadline).thenReturn(deadline)
    assertEquals(deadline, submission.deadline)
  }

  @Test
  fun `use time limit if assignment has no deadline`() {
    whenever(assignment.timeLimit).thenReturn(30L)
    assertEquals(
        submission.createdAt.plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        submission.deadline?.truncatedTo(ChronoUnit.SECONDS)
    )
  }

  @Test
  fun `use time limit if it is before assignment deadline`() {
    submission.createdAt = Instant.now()
    val deadline = submission.createdAt.plusSeconds(60)
    whenever(assignment.deadline).thenReturn(deadline)
    whenever(assignment.timeLimit).thenReturn(30L)
    assertEquals(
        submission.createdAt.plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        submission.deadline?.truncatedTo(ChronoUnit.SECONDS)
    )
  }

  @Test
  fun `use assignment deadline if it is before time limit`() {
    submission.createdAt = Instant.now()
    val deadline = Instant.now().plusSeconds(20)
    whenever(assignment.deadline).thenReturn(deadline)
    whenever(assignment.timeLimit).thenReturn(30L)
    assertEquals(deadline, submission.deadline)
  }
}
