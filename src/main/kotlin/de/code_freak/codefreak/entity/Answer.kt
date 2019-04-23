package de.code_freak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.Entity
import javax.persistence.ManyToOne

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
  var task: Task,

  /**
   * A tar archive of files that have been submitted
   */
  @Type(type = "image")
  var files: ByteArray?
) : BaseEntity() {
  init {
    if (!submission.answers.contains(this)) {
      submission.answers.add(this)
    }
  }
}
