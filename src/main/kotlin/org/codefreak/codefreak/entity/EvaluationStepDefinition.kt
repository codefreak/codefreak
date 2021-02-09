package org.codefreak.codefreak.entity

import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import javax.persistence.*

@Entity
class EvaluationStepDefinition(
  @ManyToOne(optional = false)
  var task: Task,
  var runnerName: String,
  var position: Int,
  var title: String,

  @Type(type = "json")
  @Column(length = 1024)
  @ColumnDefault("'{}'")
  var options: Map<String, Any> = mapOf()
) : BaseEntity(), Comparable<EvaluationStepDefinition> {
  var active: Boolean = true


  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name="gradeDefinition", referencedColumnName = "id")
  var gradeDefinition : GradeDefinition?=null

  /**
   * Timeout of this task in seconds
   * If null the global default timeout will be used
   */
  var timeout: Long? = null

  override fun equals(other: Any?): Boolean {
    if (other is EvaluationStepDefinition) {
      return other.id == id && other.runnerName == runnerName
    }
    return super.equals(other)
  }

  override fun compareTo(other: EvaluationStepDefinition) = position - other.position
}
