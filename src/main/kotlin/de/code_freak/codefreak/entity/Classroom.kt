package de.code_freak.codefreak.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class Classroom(
  id: Long? = null,

  /**
   * Name of this course
   */
  @Column(nullable = false)
  var name: String? = null,

  /**
   * Assignments in this course
   */
  @OneToMany(mappedBy = "classroom")
  var assignments: List<Assignment>
) : JpaPersistable<Long>(id)
