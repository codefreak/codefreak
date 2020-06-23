package org.codefreak.codefreak.service

import org.codefreak.codefreak.Env
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Service
import javax.servlet.MultipartConfigElement

@Service
class AdministrationService : BaseService() {

  @Autowired
  lateinit var env: Environment

  @Autowired
  lateinit var multipartConfigElement: MultipartConfigElement

  fun getMotd() = if (env.acceptsProfiles(Profiles.of(Env.DEMO)))
      "This is a demo system. Data may disappear at any time!"
      else null

  fun getMaxUploadSize(): Long = multipartConfigElement.maxFileSize
}
