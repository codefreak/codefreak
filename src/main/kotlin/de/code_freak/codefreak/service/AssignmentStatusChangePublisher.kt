package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.AssignmentStatus
import de.code_freak.codefreak.repository.AssignmentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.Ordered
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import javax.persistence.PostPersist
import javax.persistence.PostRemove
import javax.persistence.PostUpdate

@Component
class AssignmentStatusChangePublisher : ApplicationListener<ContextRefreshedEvent>, Ordered {
  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var taskScheduler: TaskScheduler

  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  val scheduledEvents: MutableMap<Pair<Assignment, AssignmentStatus>, ScheduledFuture<*>> = mutableMapOf()

  /**
   * Schedule all future assignment statuses on application startup
   */
  override fun onApplicationEvent(event: ContextRefreshedEvent) {
    assignmentRepository.getByOpenFromAfterOrDeadlineAfter(Instant.now()).forEach(this::scheduleAssignmentStatusEvents)
  }

  @PostPersist
  @PostUpdate
  fun onAssignmentChanged(assignment: Assignment) {
    cancelAssignmentEvents(assignment)
    scheduleAssignmentStatusEvents(assignment)
    // TODO: check if active state has been changed
  }

  @PostRemove
  fun onAssignmentRemoved(assignment: Assignment) = cancelAssignmentEvents(assignment)

  private fun cancelAssignmentEvents(assignment: Assignment) {
    cancelAssignmentStatusEvents(assignment, AssignmentStatus.OPEN)
    cancelAssignmentStatusEvents(assignment, AssignmentStatus.CLOSED)
  }

  private fun scheduleAssignmentStatusEvents(assignment: Assignment) {
    assignment.openFrom?.let { publishStatusDelayed(assignment, AssignmentStatus.OPEN, it) }
    assignment.deadline?.let { publishStatusDelayed(assignment, AssignmentStatus.CLOSED, it) }
  }

  private fun publishStatusDelayed(assignment: Assignment, status: AssignmentStatus, date: Instant) {
    scheduledEvents[Pair(assignment, status)] = taskScheduler.schedule({
      eventPublisher.publishEvent(AssignmentStatusChangedEvent(assignment.id, status))
      scheduledEvents.remove(Pair(assignment, status))
    }, date)
  }

  private fun cancelAssignmentStatusEvents(assignment: Assignment, status: AssignmentStatus) {
    scheduledEvents.remove(Pair(assignment, status))?.cancel(true)
  }

  override fun getOrder() = Ordered.LOWEST_PRECEDENCE
}
