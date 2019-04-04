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
  var assignment: Assignment,

  /**
   * List of submissions for this task
   */
  @OneToMany(mappedBy = "submission")
  var taskSubmissions: List<TaskSubmission> = ArrayList()
) : JpaPersistable() {

  /**
   * Get the submission for the given task id or null if there is no submission (yet)
   */
  fun forTask(taskId: UUID) = taskSubmissions.firstOrNull { taskSubmission -> taskSubmission.task.id == taskId }
}
