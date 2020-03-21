package de.code_freak.codefreak.entity

import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.SortedSet
import javax.persistence.CascadeType
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

  var openFrom: Instant? = null,
  var deadline: Instant? = null,
  var active: Boolean = false
) : BaseEntity() {
  /**
   * A list of tasks in this assignment ordered by their position
   */
  @OneToMany(mappedBy = "assignment", cascade = [CascadeType.REMOVE])
  @OrderBy("position ASC")
  var tasks: SortedSet<Task> = sortedSetOf<Task>()
    get() = field.sortedBy { it.position }.toSortedSet()

  val status get() = when {
    !active -> AssignmentStatus.INACTIVE
    openFrom == null || Instant.now().isBefore(openFrom) -> AssignmentStatus.ACTIVE
    deadline == null || Instant.now().isBefore(deadline) -> AssignmentStatus.OPEN
    else -> AssignmentStatus.CLOSED
  }

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  fun requireOpen() = require(status == AssignmentStatus.OPEN) { "The assignment is not open for submissions." }

  @OneToMany(mappedBy = "assignment", cascade = [CascadeType.REMOVE])
  var submissions = mutableSetOf<Submission>()
}
