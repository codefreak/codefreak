package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.Evaluation
import org.springframework.context.ApplicationEvent

class EvaluationFinishedEvent(val evaluation: Evaluation) : ApplicationEvent(evaluation)
