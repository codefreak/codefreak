package org.codefreak.cloud.companion

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Create a logger for the class this function is called in.
 * Example:
 * ```kotlin
 * class Foo {
 *   val log = logger()
 * }
 * ```
 */
inline fun <reified T> T.logger(): Logger {
  if (T::class.isCompanion) {
    return LoggerFactory.getLogger(T::class.java.enclosingClass)
  }
  return LoggerFactory.getLogger(T::class.java)
}
