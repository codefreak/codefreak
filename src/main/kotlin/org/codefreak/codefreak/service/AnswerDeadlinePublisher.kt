package org.codefreak.codefreak.service

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import javax.persistence.PostPersist
import javax.persistence.PostUpdate
import javax.persistence.PreRemove
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.EntityListener
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
@EntityListener(Answer::class)
class AnswerDeadlinePublisher {
  companion object {
    private val log = LoggerFactory.getLogger(AnswerDeadlinePublisher::class.java)
  }

  @Autowired
  private lateinit var eventPublisher: ApplicationEventPublisher

  @Autowired
  private lateinit var taskScheduler: TaskScheduler

  @Lazy
  @Autowired
  private lateinit var assignmentRepository: AssignmentRepository

  val scheduledAnswerDeadlines: MutableMap<UUID, ScheduledFuture<*>> = mutableMapOf()

  /**
   * Schedule deadlines on startup from database
   */
  @EventListener(ContextRefreshedEvent::class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  @Transactional(readOnly = true)
  fun onApplicationStartup() {
    // find all answers of still open assignments
    assignmentRepository.getByOpenFromAfterOrDeadlineAfter(Instant.now()).flatMap { assignment ->
      assignment.submissions.flatMap { submission -> submission.answers }
    }.forEach(this::scheduleAnswerDeadline)
  }

  @PostPersist
  @PostUpdate
  fun onAnswerModify(answer: Answer) {
    cancelAnswerDeadlineEvent(answer)
    scheduleAnswerDeadline(answer)
  }

  @PreRemove
  fun onAnswerRemove(answer: Answer) {
    cancelAnswerDeadlineEvent(answer)
  }

  private fun cancelAnswerDeadlineEvent(answer: Answer) {
    scheduledAnswerDeadlines.remove(answer.id)?.let {
      if (log.isDebugEnabled) {
        log.debug("Cancelling deadline event for answer ${answer.id}")
      }
      it.cancel(true)
    }
  }

  private fun scheduleAnswerDeadline(answer: Answer) {
    if (answer.task.timeLimit == null) {
      // Only trigger deadline events for answers of tasks with time limit
      // If you need to listen for regular end of assignments check the AssignmentStatusChangedEvent.
      return
    }

    val deadline = answer.deadline
    if (deadline == null || deadline <= Instant.now()) {
      // only schedule future deadlines
      return
    }
    if (log.isDebugEnabled) {
      log.debug("Scheduling deadline event for answer ${answer.id} @ $deadline")
    }
    scheduledAnswerDeadlines[answer.id] = taskScheduler.schedule({
      eventPublisher.publishEvent(AnswerDeadlineReachedEvent(answer.id))
      scheduledAnswerDeadlines.remove(answer.id)
    }, deadline)
  }
}
