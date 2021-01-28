package org.codefreak.codefreak.frontend

import javax.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View

@Component
class ReactErrorViewResolver : ErrorViewResolver, Ordered {
  @Autowired
  @Qualifier("error")
  private lateinit var errorView: View

  override fun resolveErrorView(request: HttpServletRequest?, status: HttpStatus?, model: MutableMap<String, Any>?): ModelAndView {
    return ModelAndView(errorView, model)
  }

  override fun getOrder() = Ordered.HIGHEST_PRECEDENCE
}
