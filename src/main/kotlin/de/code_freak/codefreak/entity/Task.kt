package de.code_freak.codefreak.entity

import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Task(
  /**
   * Related assignment this task belongs to
   */
  @ManyToOne
  var assignment: Assignment,

  /**
   * Position/Index in the assignment (zero-based)
   */
  @Column(nullable = false)
  var position: Long,

  /**
   * Title of the task
   */
  @Column(nullable = false)
  var title: String,

  /**
   * The task body/description of what to do
   */
  @Type( type = "text" )
  var body: String?,

  /**
   * A tar archive of files that act as boilerplate code
   */
  @Type( type = "image" )
  var files: ByteArray?,

  /**
   * A weight >=0, <=100 how the task is weighted
   * The total weight of all tasks should not be > 100
   */
  var weight: Int?
) : BaseEntity(), Comparable<Task> {
  /**
   * Evaluations that will be applied to this task
   */
  @OneToMany(mappedBy = "task")
  var requirements: MutableSet<Requirement> = mutableSetOf()

  /**
   * Same like position but one-based index
   */
  val number get() = this.position.plus(1L)

  override fun compareTo(other: Task) = position.compareTo(other.position)
}
