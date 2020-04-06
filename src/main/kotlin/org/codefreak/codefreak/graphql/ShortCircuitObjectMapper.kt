package org.codefreak.codefreak.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.core.ApplicationPart

class ShortCircuitObjectMapper : ObjectMapper() {

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> convertValue(fromValue: Any?, toValueType: Class<T>?): T {
    if (fromValue?.javaClass == toValueType) {
      return fromValue as T
    }
    if (toValueType == Array<ApplicationPart>::class.java) {
      return (fromValue as List<ApplicationPart>).toTypedArray() as T
    }
    return super.convertValue(fromValue, toValueType)
  }
}
