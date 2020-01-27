package de.code_freak.codefreak.entity

import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.SortedSet
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
   * The teacher who created this assignment
   */
  @ManyToOne
  var owner: User,

  /**
   * The classroom this assignment belongs to
   * This can be null for assignments that are shared via link
   */
  @ManyToOne(optional = true)
  var classroom: Classroom?,

  var deadline: Instant? = null
) : BaseEntity() {
  /**
   * A list of tasks in this assignment ordered by their position
   */
  @OneToMany(mappedBy = "assignment")
  @OrderBy("position ASC")
  var tasks: SortedSet<Task> = sortedSetOf<Task>()
    get() = field.sortedBy { it.position }.toSortedSet()

  val closed get() = deadline?.let { Instant.now().isAfter(deadline) } ?: false

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  fun requireNotClosed() = require(!closed) { "The assignment is already closed." }
}
