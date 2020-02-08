package de.code_freak.codefreak.service

import de.code_freak.codefreak.Env
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Service

@Service
class AdministrationService : BaseService() {

  @Autowired
  lateinit var env: Environment

  fun getMotd() = if (env.acceptsProfiles(Profiles.of(Env.DEMO)))
      "This is a demo system. Data may disappear at any time!"
      else null
}
