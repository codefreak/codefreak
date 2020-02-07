package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Answer(
  /**
   * The submission of which this task is part of
   */
  @ManyToOne
  var submission: Submission,

  /**
   * The task this submission refers to
   */
  @ManyToOne
  var task: Task
) : BaseEntity() {

  @OneToMany(mappedBy = "answer")
  var evaluations = mutableSetOf<Evaluation>()

  init {
    if (!submission.answers.contains(this)) {
      submission.answers.add(this)
    }
  }
}
