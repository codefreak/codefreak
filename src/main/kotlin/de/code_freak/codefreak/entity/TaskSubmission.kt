package de.code_freak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class TaskSubmission(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var submission: Submission,

  /**
   * The task this submission refers to
   */
  @ManyToOne
  var task: AssignmentTask,

  /**
   * A tar archive of files that have been submitted
   */
  @Type(type = "binary")
  var files: ByteArray?
) : JpaPersistable() {
  init {
    if(!submission.taskSubmissions.contains(this)) {
      submission.taskSubmissions.add(this)
    }
  }
}
