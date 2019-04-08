package de.code_freak.codefreak.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderBy

@Entity
class Assignment(
  /**
   * A title for this assignment
   * e.g. "Java Threads and Runnable"
   */
  @Column(nullable = false)
  var title: String,

  /**
   * The lecturer who created this assignment
   */
  @ManyToOne
  var owner: User,

  /**
   * The classroom this assignment belongs to
   * This can be null for assignments that are shared via link
   */
  @ManyToOne(optional = true)
  var classroom: Classroom?
) : JpaPersistable() {
  /**
   * A list of tasks in this assignment ordered by their position
   */
  @OneToMany(mappedBy = "assignment")
  @OrderBy("position ASC")
  var tasks: MutableList<AssignmentTask> = ArrayList()
}
