package de.code_freak.codefreak.entity

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.MapKeyColumn

@Entity
class Requirement(
  /**
   * The task that should be evaluated
   */
  @ManyToOne
  var task: Task,

  /**
   * The runner that is responsible for requirements
   *
   * TODO: Create a enum/registry that holds all possible evaluators – see #12
   */
  var adapter: String,

  /**
   * Configuration for the runner as key-value pairs
   */
  @ElementCollection
  @MapKeyColumn(name = "option")
  @Column(name = "value")
  var adapterConfig: Map<String, String> = HashMap()
) : BaseEntity()
