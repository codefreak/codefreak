package org.codefreak.codefreak.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Answer(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var submission: Submission,

  /**
   * The task this submission refers to
   */
  @ManyToOne
  var task: Task

) : BaseEntity() {

  init {
    if (!submission.answers.contains(this)) {
      submission.answers.add(this)
    }
  }

  @OneToMany(mappedBy = "answer", cascade = [CascadeType.REMOVE])
  var evaluations = mutableSetOf<Evaluation>()

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  @UpdateTimestamp
  var updatedAt: Instant = Instant.now()

  val deadline: Instant?
    get() {
      val assignmentDeadline = task.assignment?.deadline
      val taskDeadline = task.timeLimit?.let { createdAt.plusSeconds(it) }
      return when {
        taskDeadline == null -> assignmentDeadline
        assignmentDeadline == null -> taskDeadline
        // never allow editing after assignment deadline
        assignmentDeadline < taskDeadline -> assignmentDeadline
        else -> taskDeadline
      }
    }

  val deadlineReached get() = deadline?.let { Instant.now().isAfter(it) } ?: false

  val isEditable get() = when {
    task.assignment?.status?.equals(AssignmentStatus.OPEN) == false -> false
    deadlineReached -> false
    // no assignment or no time limit
    else -> true
  }
}
