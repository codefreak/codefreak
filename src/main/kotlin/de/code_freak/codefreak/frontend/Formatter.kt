package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.config.AppConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class Formatter {
  @Autowired
  lateinit var config: AppConfiguration

  private val instantDateTimeFormatter by lazy {
    DateTimeFormatter.ofPattern(config.l10n.dateTimeFormat)
        .withLocale(config.l10n.locale)
        .withZone(config.l10n.timeZone)
  }

  fun dateTime(instant: Instant) = instantDateTimeFormatter.format(instant)
}
