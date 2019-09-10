package de.code_freak.codefreak.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import java.time.Instant
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Evaluation(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var answer: Answer,

  @Type(type = "image")
  var filesDigest: ByteArray,

  @OneToMany
  var results: List<EvaluationResult>
) : BaseEntity() {

  @CreationTimestamp
  var createdAt: Instant = Instant.now()
}
