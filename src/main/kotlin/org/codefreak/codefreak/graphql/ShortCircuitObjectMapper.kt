package org.codefreak.codefreak.graphql

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.catalina.core.ApplicationPart

/**
 * Custom ObjectMapper brings back the short circuit behaviour of Jackson pre v2.10
 * If fromValue is already of the correct type no further conversion is done
 *
 * We need this in GraphQL to make uploads work properly
 */
class ShortCircuitObjectMapper : ObjectMapper() {
  @Suppress("UNCHECKED_CAST")
  override fun _convert(fromValue: Any?, toValueType: JavaType?): Any? {
    if (fromValue?.javaClass == toValueType) {
      return fromValue
    }
    // This should be more generic but we don't know the inner type of fromValue
    if (toValueType?.rawClass == Array<ApplicationPart>::class.java) {
      return (fromValue as List<ApplicationPart>).toTypedArray()
    }
    return super._convert(fromValue, toValueType)
  }
}
