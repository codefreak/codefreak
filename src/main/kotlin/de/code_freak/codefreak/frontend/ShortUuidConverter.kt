package de.code_freak.codefreak.frontend

import com.hsingh.shortuuid.ShortUuid
import org.springframework.core.convert.converter.Converter
import java.lang.IllegalArgumentException
import java.util.UUID

class ShortUuidConverter : Converter<String, UUID> {
  private val shortUuidBuilder = ShortUuid.Builder()

  override fun convert(source: String): UUID? {
    try {
      return UUID.fromString(shortUuidBuilder.decode(source))
    } catch (e: Exception) {
      throw IllegalArgumentException()
    }
  }
}
