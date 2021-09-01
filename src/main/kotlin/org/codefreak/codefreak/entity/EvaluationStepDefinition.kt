package org.codefreak.codefreak.entity

import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import org.hibernate.annotations.Type

@Entity
@Table(
    uniqueConstraints = [
      // prevent duplicate keys on tasks
      UniqueConstraint(columnNames = ["key", "task_id"])
    ]
)
class EvaluationStepDefinition(
  var key: String,
  @ManyToOne(optional = false)
  var task: Task,
  var position: Int,
  var title: String,
  @Type(type = "text")
  var script: String,
  @Embedded
  var report: EvaluationStepReportDefinition
) : BaseEntity(), Comparable<EvaluationStepDefinition> {
  var active: Boolean = true

  /**
   * Timeout of this task in seconds
   * If null the global default timeout will be used
   */
  var timeout: Long? = null

  override fun equals(other: Any?): Boolean {
    if (other is EvaluationStepDefinition) {
      return other.id == id && other.key == key
    }
    return super.equals(other)
  }

  override fun compareTo(other: EvaluationStepDefinition) = position - other.position

  @Embeddable
  class EvaluationStepReportDefinition(
    var format: String,
    var path: String
  )
}
