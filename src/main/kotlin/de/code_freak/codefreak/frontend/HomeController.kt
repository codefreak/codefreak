package de.code_freak.codefreak.frontend

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class HomeController : BaseController() {

  @RequestMapping("/")
  fun home(): String {
    return "home"
  }
}
