package org.codefreak.codefreak.service

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import javax.persistence.PostPersist
import javax.persistence.PostUpdate
import javax.persistence.PreRemove
import org.codefreak.codefreak.entity.EntityListener
import org.codefreak.codefreak.entity.Submission
import org.codefreak.codefreak.repository.AssignmentRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@EntityListener(Submission::class)
class SubmissionDeadlinePublisher {
  companion object {
    private val log = LoggerFactory.getLogger(SubmissionDeadlinePublisher::class.java)
  }

  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var taskScheduler: TaskScheduler

  @Lazy
  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  val scheduledDeadlines: MutableMap<UUID, ScheduledFuture<*>> = mutableMapOf()

  /**
   * Schedule deadlines on startup from database
   */
  @EventListener(ContextRefreshedEvent::class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  @Transactional(readOnly = true)
  fun onApplicationStartup() {
    // find all submissions of still open assignments
    assignmentRepository.getByOpenFromAfterOrDeadlineAfter(Instant.now()).forEach { assignment ->
      assignment.submissions.forEach(this::scheduleSubmissionDeadline)
    }
  }

  @PostPersist
  @PostUpdate
  fun onSubmissionModify(submission: Submission) {
    cancelSubmissionDeadlineEvent(submission)
    scheduleSubmissionDeadline(submission)
  }

  @PreRemove
  fun onSubmissionRemove(submission: Submission) {
    cancelSubmissionDeadlineEvent(submission)
  }

  private fun cancelSubmissionDeadlineEvent(submission: Submission) {
    scheduledDeadlines.remove(submission.id)?.let {
      if (log.isDebugEnabled) {
        log.debug("Cancelling deadline event for submission ${submission.id}")
      }
      it.cancel(true)
    }
  }

  private fun scheduleSubmissionDeadline(submission: Submission) {
    if (submission.assignment?.timeLimit == null) {
      // Only trigger deadline events for submissions with assignment and time limit
      // If you need to listen for regular end of assignments check the AssignmentStatusChangedEvent.
      return
    }

    val deadline = submission.deadline
    if (deadline == null || deadline <= Instant.now()) {
      // only schedule future deadlines
      return
    }
    if (log.isDebugEnabled) {
      log.debug("Scheduling deadline event for submission ${submission.id} @ $deadline")
    }
    scheduledDeadlines[submission.id] = taskScheduler.schedule({
      eventPublisher.publishEvent(SubmissionDeadlineReachedEvent(submission.id))
      scheduledDeadlines.remove(submission.id)
    }, deadline)
  }
}
