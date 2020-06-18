package org.codefreak.codefreak.entity

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.time.Instant
import java.time.temporal.ChronoUnit

class AnswerTest {
  @Mock
  lateinit var assignment: Assignment

  @Mock
  lateinit var task: Task

  @Mock
  lateinit var submission: Submission

  @InjectMocks
  lateinit var answer: Answer

  @Before
  fun init() {
    MockitoAnnotations.initMocks(this)
    whenever(task.assignment).thenReturn(assignment)
    doReturn(null).whenever(assignment).deadline
    doReturn(null).whenever(task).timeLimit
  }

  @Test
  fun `no deadline if neither assignment have deadline nor task has time limit`() {
    assertNull(answer.deadline)
  }

  @Test
  fun `use assignment deadline if no time limit is set`() {
    val deadline = Instant.now()
    whenever(assignment.deadline).thenReturn(deadline)
    assertEquals(deadline, answer.deadline)
  }

  @Test
  fun `use time limit if assignment has no deadline`() {
    val createdAt = Instant.now()
    whenever(task.createdAt).thenReturn(createdAt)
    whenever(task.timeLimit).thenReturn(30L)
    assertEquals(
        createdAt.plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        answer.deadline?.truncatedTo(ChronoUnit.SECONDS)
    )
  }

  @Test
  fun `use time limit if it is before deadline`() {
    val createdAt = Instant.now()
    val deadline = createdAt.plusSeconds(60)
    whenever(assignment.deadline).thenReturn(deadline)
    whenever(task.createdAt).thenReturn(createdAt)
    whenever(task.timeLimit).thenReturn(30L)
    assertEquals(
        createdAt.plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        answer.deadline?.truncatedTo(ChronoUnit.SECONDS)
    )
  }

  @Test
  fun `use deadline if it is before time limit`() {
    val createdAt = Instant.now()
    val deadline = Instant.now().plusSeconds(20)
    whenever(assignment.deadline).thenReturn(deadline)
    whenever(task.createdAt).thenReturn(createdAt)
    whenever(task.timeLimit).thenReturn(30L)
    assertEquals(deadline, answer.deadline)
  }

  @Test
  fun `answer is editable by default`() {
    assertTrue(answer.isEditable)
  }

  @Test
  fun `answer is editable if task has no assignment`() {
    whenever(task.assignment).thenReturn(null)
    assertTrue(answer.isEditable)
  }

  @Test
  fun `answer is editable if assignment is open`() {
    whenever(task.assignment?.status).thenReturn(AssignmentStatus.OPEN)
    assertTrue(answer.isEditable)
    whenever(task.assignment?.status).thenReturn(AssignmentStatus.CLOSED)
    assertFalse(answer.isEditable)
  }

  @Test
  fun `answer is not editable if deadline has been reached`() {
    whenever(assignment.deadline).thenReturn(Instant.now().minusSeconds(30))
    assertFalse(answer.isEditable)
    whenever(assignment.deadline).thenReturn(Instant.now().plusSeconds(30))
    assertTrue(answer.isEditable)
  }
}
