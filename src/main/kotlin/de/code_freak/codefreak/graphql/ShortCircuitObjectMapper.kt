package de.code_freak.codefreak.graphql

import com.fasterxml.jackson.databind.ObjectMapper

class ShortCircuitObjectMapper : ObjectMapper() {

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> convertValue(fromValue: Any?, toValueType: Class<T>?): T {
    if (fromValue?.javaClass == toValueType) {
      return fromValue as T
    }
    return super.convertValue(fromValue, toValueType)
  }
}
