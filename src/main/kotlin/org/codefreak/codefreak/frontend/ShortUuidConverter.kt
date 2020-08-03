package org.codefreak.codefreak.frontend

import com.hsingh.shortuuid.ShortUuid
import java.util.UUID
import org.springframework.core.convert.converter.Converter

class ShortUuidConverter : Converter<String, UUID> {
  private val shortUuidBuilder = ShortUuid.Builder()

  override fun convert(source: String): UUID? {
    // ShortUuidBuilder will also eat regular UUIDs and converts them to very different UUIDs
    // let's try to parse the string as normal UUID first
    try {
      return UUID.fromString(source)
    } catch (e: Exception) {
      // okay not a valid UUID
    }

    try {
      return UUID.fromString(shortUuidBuilder.decode(source))
    } catch (e: Exception) {
      throw IllegalArgumentException()
    }
  }
}
