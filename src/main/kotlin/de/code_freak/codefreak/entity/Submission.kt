package de.code_freak.codefreak.entity

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Submission(
  /**
   * The user that handed in this submissions
   * TODO: remove optional if authentication is implemented
   */
  @ManyToOne(optional = true)
  var user: User? = null,

  /**
   * The assignment this submission belongs to
   */
  @ManyToOne
  var assignment: Assignment
) : BaseEntity() {
  /**
   * List of answers in this submission
   */
  @OneToMany(mappedBy = "submission")
  var answers: MutableSet<Answer> = mutableSetOf()

  /**
   * Get the answer for the given task id or null if there is no submission (yet)
   */
  fun getAnswerForTask(taskId: UUID) = answers.firstOrNull { answer -> answer.task.id == taskId }
}
