package org.codefreak.codefreak.entity

import java.time.Instant
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import org.hibernate.annotations.CreationTimestamp
import javax.persistence.EntityListeners

@Entity
@EntityListeners(SpringEntityListenerAdapter::class)
class Submission(
  /**
   * The user that handed in this submissions
   */
  @ManyToOne
  var user: User,

  /**
   * The assignment this submission belongs to.
   * Null means this is used for testing mode in the task pool.
   */
  @ManyToOne
  var assignment: Assignment?
) : BaseEntity() {
  /**
   * List of answers in this submission
   */
  @OneToMany(mappedBy = "submission", cascade = [CascadeType.REMOVE])
  var answers: MutableSet<Answer> = mutableSetOf()

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  val deadline: Instant?
    get() {
      val assignmentDeadline = assignment?.deadline
      val submissionDeadline = assignment?.timeLimit?.let { createdAt.plusSeconds(it) }
      return when {
        submissionDeadline == null -> assignmentDeadline
        assignmentDeadline == null -> submissionDeadline
        // never allow editing after assignment deadline
        assignmentDeadline < submissionDeadline -> assignmentDeadline
        else -> submissionDeadline
      }
    }

  val deadlineReached get() = deadline?.let { Instant.now().isAfter(it) } ?: false

  fun getAnswer(taskId: UUID) = answers.firstOrNull { it.task.id == taskId }
}
