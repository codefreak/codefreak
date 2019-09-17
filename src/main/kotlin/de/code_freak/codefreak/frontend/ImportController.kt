package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.auth.Authority
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ImportController : BaseController() {

  @Secured(Authority.ROLE_TEACHER)
  @GetMapping("/import")
  fun getImportForms() = "import"
}
