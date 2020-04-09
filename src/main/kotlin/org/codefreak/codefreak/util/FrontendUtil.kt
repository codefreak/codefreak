package org.codefreak.codefreak.util

import org.codefreak.codefreak.entity.User
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

object FrontendUtil {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getRequest() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

  fun getCurrentUser(): User {
    val principal = SecurityContextHolder.getContext().authentication?.principal
    if (principal is User) {
      return principal
    }
    log.warn("Expected instance of User but received $principal instead.")
    throw AccessDeniedException("Not authenticated")
  }

  fun getUriBuilder() = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null)
}
