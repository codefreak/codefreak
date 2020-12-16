package org.codefreak.codefreak.service

import java.util.UUID
import org.codefreak.codefreak.entity.AssignmentStatus
import org.codefreak.codefreak.entity.Evaluation
import org.codefreak.codefreak.entity.EvaluationStep
import org.codefreak.codefreak.entity.EvaluationStepStatus
import org.springframework.context.ApplicationEvent

class EvaluationStatusUpdatedEvent(val evaluation: Evaluation, val status: EvaluationStepStatus) : ApplicationEvent(evaluation)

class EvaluationStepStatusUpdatedEvent(val evaluationStep: EvaluationStep, val status: EvaluationStepStatus) : ApplicationEvent(evaluationStep)

class AssignmentStatusChangedEvent(val assignmentId: UUID, val status: AssignmentStatus) : ApplicationEvent(assignmentId)

class SubmissionDeadlineReachedEvent(val submissionId: UUID) : ApplicationEvent(submissionId)
