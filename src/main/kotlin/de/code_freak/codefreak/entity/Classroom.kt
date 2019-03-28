package de.code_freak.codefreak.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class Classroom(
  /**
   * Name of this course
   */
  @Column(nullable = false)
  var name: String,

  /**
   * Assignments in this course
   */
  @OneToMany(mappedBy = "classroom")
  var assignments: List<Assignment> = ArrayList()
) : JpaPersistable()
