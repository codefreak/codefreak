package de.code_freak.codefreak.frontend

import org.springframework.stereotype.Component

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Component
class Formatter {
  private val instantDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
      .withLocale(Locale.UK)
      .withZone(ZoneId.systemDefault())

  fun dateTime(instant: Instant) = instantDateTimeFormatter.format(instant)
}
