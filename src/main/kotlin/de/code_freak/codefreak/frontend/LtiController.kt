package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.service.AssignmentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/lti")
class LtiController {
  @Autowired
  lateinit var assignmentService: AssignmentService

  /**
   * Responsible for LTI Deep Linking requests
   * Shows a list of assignments that can be selected and linked in an LMS
   */
  @GetMapping("/deep-link")
  fun launch(model: Model): String {
    model.addAttribute("assignments", assignmentService.findAllAssignments())
    return "lti/deep-link"
  }
}
