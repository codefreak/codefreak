package org.codefreak.codefreak.service

import org.codefreak.codefreak.entity.Assignment
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.repository.AssignmentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import javax.persistence.PostPersist
import javax.persistence.PostUpdate
import javax.persistence.PreRemove

@Component
class AssignmentStatusChangePublisher {
  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var taskScheduler: TaskScheduler

  /**
   * Inject lazy because this is a cyclic dependency (sorry)
   */
  @Lazy
  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  val scheduledEvents: MutableMap<Pair<UUID, AssignmentStatus>, ScheduledFuture<*>> = mutableMapOf()

  /**
   * Schedule all future assignment statuses on application startup
   */
  @EventListener(ContextRefreshedEvent::class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  fun onApplicationStartup() {
    assignmentRepository.getByOpenFromAfterOrDeadlineAfter(Instant.now()).forEach(this::scheduleAssignmentStatusEvents)
  }

  @PostPersist
  @PostUpdate
  fun onAssignmentChanged(assignment: Assignment) {
    cancelAssignmentEvents(assignment)
    val status = assignment.status
    if (status >= AssignmentStatus.ACTIVE) {
      scheduleAssignmentStatusEvents(assignment)
    }
    // trigger instant status events for ACTIVE and INACTIVE assignments
    if (status <= AssignmentStatus.ACTIVE) {
      eventPublisher.publishEvent(AssignmentStatusChangedEvent(assignment.id, status))
    }
  }

  @PreRemove
  fun onAssignmentRemove(assignment: Assignment) = cancelAssignmentEvents(assignment)

  private fun cancelAssignmentEvents(assignment: Assignment) {
    cancelAssignmentStatusEvents(assignment, AssignmentStatus.OPEN)
    cancelAssignmentStatusEvents(assignment, AssignmentStatus.CLOSED)
  }

  private fun scheduleAssignmentStatusEvents(assignment: Assignment) {
    assignment.openFrom?.let { publishStatusDelayed(assignment, AssignmentStatus.OPEN, it) }
    assignment.deadline?.let { publishStatusDelayed(assignment, AssignmentStatus.CLOSED, it) }
  }

  @Synchronized
  private fun publishStatusDelayed(assignment: Assignment, status: AssignmentStatus, date: Instant) {
    if (date <= Instant.now().minusSeconds(60)) {
      // only schedule past events if they are not older than 1min
      // this is useful if an assignment status was just changed (a few ms ago) or if the application
      // just crashed and possibly missed to trigger some events
      return
    }
    scheduledEvents[Pair(assignment.id, status)] = taskScheduler.schedule({
      eventPublisher.publishEvent(AssignmentStatusChangedEvent(assignment.id, status))
      scheduledEvents.remove(Pair(assignment.id, status))
    }, date)
  }

  private fun cancelAssignmentStatusEvents(assignment: Assignment, status: AssignmentStatus) {
    scheduledEvents.remove(Pair(assignment.id, status))?.cancel(true)
  }
}
