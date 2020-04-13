package org.codefreak.codefreak.entity

import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class EvaluationStepDefinition(
  @ManyToOne(optional = false)
  var task: Task,
  var runnerName: String,
  var position: Int,

  @Type(type = "json")
  @Column(length = 1024)
  @ColumnDefault("'{}'")
  var options: Map<String, Any> = mapOf()
) : BaseEntity() {
  var active: Boolean = true
}
