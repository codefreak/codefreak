package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.http.NotFoundException
import de.code_freak.codefreak.service.AssignmentManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Controller
class AssignmentController : BaseController() {
  @Autowired
  lateinit var assignmentManager: AssignmentManager

  @GetMapping("/assignments/{id}")
  fun getAssignment(
    @PathVariable("id") assignmentId: UUID,
    model: Model
  ): String {
    val assignment = assignmentManager.findAssignment(assignmentId).orElseThrow {
      NotFoundException("Assignment not found")
    }

    model.addAttribute("assignment", assignment)
    return "assignment"
  }
}
