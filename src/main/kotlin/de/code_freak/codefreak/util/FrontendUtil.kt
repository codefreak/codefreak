package de.code_freak.codefreak.util

import de.code_freak.codefreak.auth.AppUser
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StreamUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.InputStream

object FrontendUtil {
  fun getRequest() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

  fun getCurrentUser() = SecurityContextHolder.getContext().authentication.principal as AppUser

  /**
   * Does not close the input stream!
   */
  fun streamResponse(inputStream: InputStream) = StreamingResponseBody { StreamUtils.copy(inputStream, it) }
}
