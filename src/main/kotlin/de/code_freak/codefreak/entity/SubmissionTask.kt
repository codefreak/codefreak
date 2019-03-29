package de.code_freak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.Entity
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
  @Type( type = "binary" )
  var files: ByteArray
) : JpaPersistable()
