package de.code_freak.codefreak.frontend

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class ReactFrontendForwardController : BaseController() {

  /**
   * React Router uses HTML5's history API.
   * This controller will forward all requests to the React frontend served at /
   */
  @RequestMapping("/**/[^.]*")
  fun frontendForward() = "forward:/"
}