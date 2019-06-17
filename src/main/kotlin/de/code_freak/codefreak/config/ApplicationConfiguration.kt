package de.code_freak.codefreak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration("app")
@PropertySource("git.properties", ignoreResourceNotFound = true)
class ApplicationConfiguration {
  @Value("\${git.closest.tag.name:}")
  var version: String = ""
    get() = if (field.isBlank()) "canary" else field

  @Value("\${git.commit.id:}")
  lateinit var gitHash: String

  @Value("\${git.commit.id.abbrev:}")
  lateinit var gitHashAbbr: String
}
