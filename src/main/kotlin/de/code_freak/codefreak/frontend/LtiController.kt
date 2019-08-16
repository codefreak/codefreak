package de.code_freak.codefreak.frontend

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/lti")
class LtiController {
  @RequestMapping("/launch", method = [RequestMethod.GET, RequestMethod.POST])
  fun launch(request: HttpServletRequest, response: HttpServletResponse): String {
    return "redirect:/"
  }
}
