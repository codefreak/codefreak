package org.codefreak.codefreak.service

import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.service.evaluation.PendingEvaluationStatus
import org.springframework.context.ApplicationEvent
import java.util.UUID

class EvaluationFinishedEvent(val evaluation: Evaluation) : ApplicationEvent(evaluation)

class PendingEvaluationUpdatedEvent(val answerId: UUID, val status: PendingEvaluationStatus) : ApplicationEvent(answerId)

class AssignmentStatusChangedEvent(val assignmentId: UUID, val status: AssignmentStatus) : ApplicationEvent(assignmentId)
