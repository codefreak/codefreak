package org.codefreak.codefreak.entity

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@EntityListeners(SpringEntityListenerAdapter::class)
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

  @OneToMany(mappedBy = "answer", cascade = [CascadeType.ALL])
  var evaluations = mutableSetOf<Evaluation>()

  /**
   * All grades related to their respective evaluations are mapped.
   * This makes it easier to get the best grade of an answer for the scoreboard
   * Otherwise too much cascading
   */
  @OneToMany(mappedBy = "answer", cascade = [CascadeType.ALL])
  var grades = mutableSetOf<Grade>()

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  @UpdateTimestamp
  var updatedAt: Instant = Instant.now()

  val isEditable
    get() = when {
      // only editable if the assignment is explicitly open
      task.assignment?.status?.equals(AssignmentStatus.OPEN) == false -> false
      submission.deadlineReached -> false
      // no assignment, assignment is open or no time limit -> allow to edit
      else -> true
    }
}
