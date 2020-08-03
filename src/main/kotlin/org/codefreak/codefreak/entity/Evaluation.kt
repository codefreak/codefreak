package org.codefreak.codefreak.entity

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Transient
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

  var evaluationSettingsFrom: Instant
) : BaseEntity() {
  @OneToMany(mappedBy = "evaluation", cascade = [CascadeType.ALL], orphanRemoval = true)
  var evaluationSteps = mutableSetOf<EvaluationStep>()

  /**
   * Represents the worst result from all steps
   * If no evaluation has been run or all results are unknown it is successful by default
   */
  val stepsResultSummary: EvaluationStep.EvaluationStepResult
    @Transient
    get() = evaluationSteps.fold(EvaluationStep.EvaluationStepResult.SUCCESS) { acc, step ->
      val result = step.result
      when {
        result != null && result > acc -> result
        else -> acc
      }
    }

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  fun addStep(step: EvaluationStep) {
    evaluationSteps.add(step)
    step.evaluation = this
  }
}
