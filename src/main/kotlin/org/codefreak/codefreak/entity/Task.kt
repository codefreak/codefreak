package org.codefreak.codefreak.entity

import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.MapKey
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp

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
  var weight: Int? = null
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

  @UpdateTimestamp
  var updatedAt: Instant = Instant.now()

  @OneToMany(
    mappedBy = "task",
    cascade = [CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE],
    orphanRemoval = true
  )
  @OrderBy("position ASC")
  @MapKey(name = "key")
  var evaluationStepDefinitions: MutableMap<String, EvaluationStepDefinition> = linkedMapOf()
    set(value) {
      field = value.values.sorted().associateBy { it.key }.toMutableMap()
    }

  /**
   * One or more files that should be initially opened in the editor
   */
  @Type(type = "json")
  @Column(length = 1024)
  @ColumnDefault("'[]'")
  var defaultFiles: List<String>? = null

  /**
   * Command that should be used to execute the source code
   */
  @Column(length = 1048576)
  @Lob
  var runCommand: String? = null

  /**
   * Full qualified name of a custom workspace image
   */
  var customWorkspaceImage: String? = null

  fun addEvaluationStepDefinition(stepDefinition: EvaluationStepDefinition) {
    if (evaluationStepDefinitions.putIfAbsent(stepDefinition.key, stepDefinition) != null) {
      throw IllegalStateException(
        "There is already an evaluation step with the name of '${stepDefinition.key} defined on this task."
      )
    }
  }

  fun deleteEvaluationStepDefinition(stepDefinition: EvaluationStepDefinition) {
    evaluationStepDefinitions.remove(stepDefinition.key)
    // update position of all following elements
    evaluationStepDefinitions.values.filter { it.position > stepDefinition.position }.onEach { it.position-- }
  }

  var evaluationSettingsChangedAt: Instant = Instant.now()

  override fun compareTo(other: Task) = position.compareTo(other.position)
}
