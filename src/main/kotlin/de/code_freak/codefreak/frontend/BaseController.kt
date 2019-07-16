package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.User
import de.code_freak.codefreak.util.FrontendUtil

abstract class BaseController {
  protected val user: User
    get() = FrontendUtil.getCurrentUser().entity
}
