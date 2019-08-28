package de.code_freak.codefreak.frontend

import org.springframework.web.bind.annotation.ResponseBody

/**
 * Denotes a REST handler method. This is to distinguish them from non-REST handlers that use @[ResponseBody],
 * e.g. for error handling.
 */
@ResponseBody
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RestHandler
