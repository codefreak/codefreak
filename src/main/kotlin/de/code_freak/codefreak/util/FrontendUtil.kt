package de.code_freak.codefreak.util

import de.code_freak.codefreak.entity.User
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

object FrontendUtil {
  fun getRequest() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

  fun getCurrentUser() = SecurityContextHolder.getContext().authentication.principal as User

  fun getUriBuilder() = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null)
}
