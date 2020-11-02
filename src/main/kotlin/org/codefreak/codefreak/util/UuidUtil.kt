package org.codefreak.codefreak.util

import java.util.UUID

object UuidUtil {

  /**
   * Tries to parse the given input to a UUID.
   *
   * @param input the input to be parsed
   * @return if the input is can be parsed to a UUID return the UUID, null otherwise
   */
  fun parse(input: Any?): UUID? {
    if (input is UUID) return input
    if (input !is String || input.isEmpty()) return null
    return try {
      UUID.fromString(input)
    } catch (e: IllegalArgumentException) {
      null
    }
  }
}
