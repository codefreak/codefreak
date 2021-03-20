package org.codefreak.codefreak.entity

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

@Entity
@EntityListeners(SpringEntityListenerAdapter::class)
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
  @ColumnDefault("false")
  var active: Boolean = false
) : BaseEntity() {
  /**
   * A list of tasks in this assignment ordered by their position
   */
  @OneToMany(mappedBy = "assignment", cascade = [CascadeType.REMOVE])
  @OrderBy("position ASC")
  var tasks: MutableSet<Task> = sortedSetOf()
    get() = field.sortedBy { it.position }.toSortedSet()

  val status get() = when {
    !active -> AssignmentStatus.INACTIVE
    openFrom == null || Instant.now().isBefore(openFrom) -> AssignmentStatus.ACTIVE
    deadline == null || Instant.now().isBefore(deadline) -> AssignmentStatus.OPEN
    else -> AssignmentStatus.CLOSED
  }

  /**
   * Let the teacher enable/disable the scoreboard
   */
  var scoreboard: Boolean = false

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  @UpdateTimestamp
  var updatedAt: Instant = Instant.now()

  @OneToMany(mappedBy = "assignment", cascade = [CascadeType.REMOVE])
  var submissions = mutableSetOf<Submission>()

  /**
   * Optional time limit for this assignments in seconds.
   * If a student starts working on the assignment he has e.g. 900sec = 15min time
   * to finish all tasks of the assignment. After the time limit is reached the cannot modify
   * his answers anymore.
   */
  var timeLimit: Long? = null
}
