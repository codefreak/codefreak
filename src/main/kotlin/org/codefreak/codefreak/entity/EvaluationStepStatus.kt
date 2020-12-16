package org.codefreak.codefreak.entity

/**
 * Represents the chronological status of an evaluation step
 * Please remain the order!
 */
enum class EvaluationStepStatus {
  /**
   * A pending step is the initial state when the step is placed in the database.
   */
  PENDING,

  /**
   * A queued step has been picked up by the evaluation processing system and will be running soon.
   */
  QUEUED,

  /**
   * Running evaluation steps are currently being processed
   */
  RUNNING,

  /**
   * A finished evaluation step has been processed by an evaluation runner.
   * All following statuses are also considered as "finished"
   */
  FINISHED,

  /**
   * Canceled means the automated processing has been interrupted or skipped completely.
   * The result of the evaluation step might be incomplete.
   */
  CANCELED
}
