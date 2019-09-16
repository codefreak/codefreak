package de.code_freak.codefreak.frontend

import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

@Controller
class HomeController : BaseController() {

  @RequestMapping("/")
  fun home() = "redirect:/assignments"

  @RequestMapping("/login")
  fun login(request: HttpServletRequest, session: HttpSession, model: Model): String {
    val params = request.parameterMap
    if (params.containsKey("error")) {
      val ex = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) as AuthenticationException?
      model.addAttribute("error", ex?.message)
    } else if (params.containsKey("logout")) {
      model.addAttribute("success", "You were logged out successfully!")
    }
    return "login"
  }
}
