package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class SubmissionTask(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var submission: Submission? = null,

  /**
   * A tar archive of files that have been submitted
   */
  var files: ByteArray? = null
) : JpaPersistable()
