package de.code_freak.codefreak.service.evaluation

import org.springframework.beans.factory.annotation.Qualifier

@Qualifier
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EvaluationQualifier
