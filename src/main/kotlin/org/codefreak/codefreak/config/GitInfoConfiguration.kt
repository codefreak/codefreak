package org.codefreak.codefreak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration("gitInfo")
@PropertySource("git.properties", ignoreResourceNotFound = true)
class GitInfoConfiguration {
  @Value("\${git.closest.tag.name:}")
  var version: String = ""
    get() = if (field.isBlank()) "canary" else field

  @Value("\${git.commit.id:}")
  lateinit var hash: String

  @Value("\${git.commit.id.abbrev:}")
  lateinit var hashAbbr: String
}
