package de.code_freak.codefreak.entity

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class TaskEvaluation(
  id: Long? = null,

  /**
   * The task that should be evaluated
   */
  @ManyToOne
  var task: AssignmentTask? = null,

  /**
   * The adapter that is responsible for evaluation
   *
   * TODO: Create a enum/registry that holds all possible evaluators – see #12
   */
  var adapter: String? = null
) : JpaPersistable<Long>(id)
