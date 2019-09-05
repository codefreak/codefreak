package de.code_freak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.Entity

@Entity
class EvaluationResult(
  var runnerName: String,

  @Type(type = "image")
  var content: ByteArray,

  var position: Int,

  var error: Boolean = false
) : BaseEntity()
