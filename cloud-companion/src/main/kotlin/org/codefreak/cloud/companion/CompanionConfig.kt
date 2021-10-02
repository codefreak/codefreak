package org.codefreak.cloud.companion

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component("config")
@ConfigurationProperties(prefix = "companion")
class CompanionConfig {
  var projectFilesPath = "/home/runner/project"
}
