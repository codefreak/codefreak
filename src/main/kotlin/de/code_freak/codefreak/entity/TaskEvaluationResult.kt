package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class TaskEvaluationResult(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var taskSubmission: TaskSubmission,

  /**
   * Link to the evaluation that has been used to create the result
   */
  @ManyToOne
  var evaluation: TaskEvaluation,

  /**
   * The result value that was determined by the evaluation
   */
  var result: Long
) : JpaPersistable()
