package org.codefreak.codefreak.entity

import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AnswerTest {
  @Mock
  lateinit var assignment: Assignment

  @Mock
  lateinit var task: Task

  @Mock
  lateinit var submission: Submission

  @InjectMocks
  lateinit var answer: Answer

  @Test
  fun `answer is editable by default`() {
    Assertions.assertTrue(answer.isEditable)
  }

  @Test
  fun `answer is editable by default if task has no assignment`() {
    Assertions.assertTrue(answer.isEditable)
  }

  @Test
  fun `answer is editable only if assignment is open`() {
    whenever(assignment.status).thenReturn(AssignmentStatus.OPEN)
    whenever(task.assignment).thenReturn(assignment)
    Assertions.assertTrue(answer.isEditable)
    whenever(assignment.status).thenReturn(AssignmentStatus.CLOSED)
    Assertions.assertFalse(answer.isEditable)
  }

  @Test
  fun `answer is not editable if deadline has been reached`() {
    whenever(submission.deadlineReached).thenReturn(true)
    Assertions.assertFalse(answer.isEditable)
    whenever(submission.deadlineReached).thenReturn(false)
    Assertions.assertTrue(answer.isEditable)
  }
}
