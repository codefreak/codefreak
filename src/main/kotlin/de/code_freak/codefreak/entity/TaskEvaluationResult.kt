package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class TaskEvaluationResult(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var submissionTask: SubmissionTask? = null,

  /**
   * Link to the evaluation that has been used to create the result
   */
  @ManyToOne
  var evaluation: TaskEvaluation? = null,

  /**
   * A tar archive of files that have been submitted
   */
  var result: Long? = null
) : JpaPersistable()
