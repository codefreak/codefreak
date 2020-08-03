package org.codefreak.codefreak.entity

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type

@Entity
class Task(
  /**
   * Related assignment this task belongs to
   */
  @ManyToOne
  var assignment: Assignment?,

  /**
   * The teacher who created this assignment
   */
  @ManyToOne
  var owner: User,

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
  @Type(type = "text")
  var body: String? = null,

  /**
   * A weight >=0, <=100 how the task is weighted
   * The total weight of all tasks should not be > 100
   */
  var weight: Int? = null,

  /**
   * Time limit for this task in seconds.
   * If a student starts working on the task he has e.g. 900sec = 15min time
   * to finish this task. After the time limit is reached the cannot modify
   * his answer.
   */
  var timeLimit: Long? = null
) : BaseEntity(), Comparable<Task> {
  @OneToMany(mappedBy = "task", cascade = [CascadeType.REMOVE])
  var answers: MutableSet<Answer> = mutableSetOf()

  @Type(type = "json")
  @Column(length = 1024)
  @ColumnDefault("'[]'")
  var hiddenFiles: List<String> = listOf()

  @Type(type = "json")
  @Column(length = 1024)
  @ColumnDefault("'[]'")
  var protectedFiles: List<String> = listOf()

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  @OneToMany(mappedBy = "task", cascade = [CascadeType.REMOVE])
  var evaluationStepDefinitions: MutableSet<EvaluationStepDefinition> = mutableSetOf()

  var evaluationSettingsChangedAt: Instant = Instant.now()

  override fun compareTo(other: Task) = position.compareTo(other.position)
}
