package org.codefreak.codefreak.frontend

import javax.servlet.http.HttpServletRequest
import org.codefreak.codefreak.Env
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

/**
 * Custom ErrorController that includes the stacktrace if we are in a development environment
 * By default the stacktrace is disabled via the application.yml
 */
@Component
@Profile(Env.DEV)
class DevErrorController(
  @Autowired errorAttributes: ErrorAttributes,
  @Autowired serverProperties: ServerProperties,
  @Autowired errorViewResolvers: ObjectProvider<ErrorViewResolver>
) : BasicErrorController(errorAttributes, serverProperties.error, errorViewResolvers.toList()) {

  override fun getErrorAttributeOptions(request: HttpServletRequest?, mediaType: MediaType?): ErrorAttributeOptions {
    return super.getErrorAttributeOptions(request, mediaType).including(ErrorAttributeOptions.Include.STACK_TRACE)
  }
}
