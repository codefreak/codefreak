package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Evaluation
import de.code_freak.codefreak.service.evaluation.PendingEvaluationStatus
import org.springframework.context.ApplicationEvent
import java.util.UUID

class EvaluationFinishedEvent(val evaluation: Evaluation) : ApplicationEvent(evaluation)

class PendingEvaluationUpdatedEvent(val answerId: UUID, val status: PendingEvaluationStatus) : ApplicationEvent(answerId)
