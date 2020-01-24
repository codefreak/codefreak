package de.code_freak.codefreak.service

import de.code_freak.codefreak.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.session.SessionInformation
import org.springframework.security.core.session.SessionRegistry
import org.springframework.stereotype.Service

@Service
class SessionService : BaseService() {

  @Autowired
  private lateinit var sessionRegistry: SessionRegistry

  fun getSession(sessionId: String): SessionInformation? = sessionRegistry.getSessionInformation(sessionId)

  fun registerNewSession(sessionId: String, user: User) = sessionRegistry.registerNewSession(sessionId, user)
}
