package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.entity.DemoUser
import de.code_freak.codefreak.repository.DemoUserRepository
import de.code_freak.codefreak.service.EntityNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*
import javax.servlet.http.HttpServletRequest

@Controller
class DemoUserController: BaseController() {

  @Autowired
  private lateinit var demoUserRepository: DemoUserRepository

  @GetMapping("/register")
  fun register(model: Model): String {
    model["demoUser"] = DemoUser("", "")
    return "demo-register"
  }

  @PostMapping
  fun registerSubmit(@ModelAttribute demoUserInput: DemoUser, model: Model): String {
    val demoUser = demoUserRepository.save(demoUserInput)
    model["email"] = demoUser.email
    val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
    val loginUrl = baseUrl + "/login/" + demoUser.id
    // TODO: send email
    model["url"] = loginUrl
    return "demo-register-success"
  }

  @GetMapping("/login/{demoUserId}")
  fun login(@PathVariable("demoUserId") demoUserId: UUID, request: HttpServletRequest, model: Model): String {
    val demoUser = demoUserRepository.findById(demoUserId).orElseThrow { EntityNotFoundException() }
    request.session.setAttribute("demo-user-id", demoUser.id)
    model["email"] = demoUser.email
    return "demo-login"
  }

}
