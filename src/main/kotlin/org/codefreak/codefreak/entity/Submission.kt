package org.codefreak.codefreak.entity

import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
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

  fun getAnswer(taskId: UUID) = answers.firstOrNull { it.task.id == taskId }
}
