package de.code_freak.codefreak.entity

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
   * The assignment this submission belongs to
   */
  @ManyToOne
  var assignment: Assignment
) : BaseEntity() {
  /**
   * List of answers in this submission
   */
  @OneToMany(mappedBy = "submission", cascade = [CascadeType.ALL])
  var answers: MutableSet<Answer> = mutableSetOf()

  fun getAnswer(taskId: UUID) = answers.firstOrNull { it.task.id == taskId }
}
