package org.codefreak.codefreak.entity

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Transient
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type

@Entity
class Evaluation(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var answer: Answer,

  @Type(type = "image")
  var filesDigest: ByteArray,

  @ColumnDefault("1970-01-01 00:00:00")
  var evaluationSettingsFrom: Instant
) : BaseEntity() {
  @OneToMany(mappedBy = "evaluation", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  var evaluationSteps = mutableSetOf<EvaluationStep>()

  /**
   * Represents the worst result from all steps
   * If no evaluation has been run or all results are unknown it is successful by default
   */
  val stepsResultSummary: EvaluationStepResult
    @Transient
    get() = evaluationSteps.fold(EvaluationStepResult.SUCCESS) { acc, step ->
      val result = step.result
      when {
        result != null && result > acc -> result
        else -> acc
      }
    }

  val stepStatusSummary: EvaluationStepStatus
    @Transient
    get() = when {
      evaluationSteps.any { it.status == EvaluationStepStatus.CANCELED } -> EvaluationStepStatus.CANCELED
      evaluationSteps.any { it.status == EvaluationStepStatus.RUNNING } -> EvaluationStepStatus.RUNNING
      evaluationSteps.any { it.status == EvaluationStepStatus.QUEUED } -> EvaluationStepStatus.QUEUED
      evaluationSteps.all { it.status == EvaluationStepStatus.FINISHED } -> EvaluationStepStatus.FINISHED
      else -> EvaluationStepStatus.PENDING
    }

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  fun addStep(step: EvaluationStep) {
    evaluationSteps.add(step)
    step.evaluation = this
  }
}
