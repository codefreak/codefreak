package org.codefreak.codefreak.service

import javax.servlet.MultipartConfigElement
import org.codefreak.codefreak.Env
import org.codefreak.codefreak.config.AppConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Service

@Service
class AdministrationService : BaseService() {

  @Autowired
  private lateinit var env: Environment

  @Autowired
  private lateinit var config: AppConfiguration

  @Autowired
  private lateinit var multipartConfigElement: MultipartConfigElement

  fun getMotd() = if (env.acceptsProfiles(Profiles.of(Env.DEMO)))
      "This is a demo system. Data may disappear at any time!"
      else null

  fun getMaxUploadSize(): Long = multipartConfigElement.maxFileSize

  fun getDefaultEvaluationTimeout(): Long = config.evaluation.defaultTimeout
}
