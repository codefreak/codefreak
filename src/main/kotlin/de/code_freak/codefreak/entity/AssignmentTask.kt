package de.code_freak.codefreak.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class AssignmentTask(
  id: Long? = null,

  /**
   * Related assignment this task belongs to
   */
  @ManyToOne
  var assignment: Assignment? = null,

  /**
   * Position/Index in the assignment (zero-based)
   */
  @Column(nullable = false)
  var position: Long? = null,

  /**
   * Title of the task
   */
  @Column(nullable = false)
  var title: String? = null,

  /**
   * The task body/description of what to do
   */
  @Column(nullable = false, columnDefinition = "text")
  var body: String? = null,

  /**
   * A tar archive of files that act as boilerplate code
   */
  @Column(nullable = false)
  var files: ByteArray? = null,

  /**
   * A weight >=0, <=100 how the task is weighted
   * The total weight of all tasks should not be > 100
   */
  @Column(nullable = false)
  var weight: Int? = null
) : JpaPersistable<Long>(id) {

  /**
   * Same like position but one-based index
   */
  val number get() = this.position?.plus(1L)
}
