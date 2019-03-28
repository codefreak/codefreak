package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.Lob
import javax.persistence.ManyToOne

@Entity
class SubmissionTask(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var submission: Submission,

  /**
   * A tar archive of files that have been submitted
   */
  @Lob
  var files: ByteArray
) : JpaPersistable()
