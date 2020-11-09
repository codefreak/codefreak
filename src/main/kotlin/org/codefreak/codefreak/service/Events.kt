package org.codefreak.codefreak.service

import java.util.UUID
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.service.evaluation.PendingEvaluationStatus
import org.springframework.context.ApplicationEvent

class EvaluationFinishedEvent(val evaluation: Evaluation) : ApplicationEvent(evaluation)

class PendingEvaluationUpdatedEvent(val answerId: UUID, val status: PendingEvaluationStatus) : ApplicationEvent(answerId)

class AssignmentStatusChangedEvent(val assignmentId: UUID, val status: AssignmentStatus) : ApplicationEvent(assignmentId)

class SubmissionDeadlineReachedEvent(val submissionId: UUID) : ApplicationEvent(submissionId)
