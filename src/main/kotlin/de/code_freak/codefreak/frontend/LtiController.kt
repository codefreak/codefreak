package de.code_freak.codefreak.frontend

import de.code_freak.codefreak.service.LtiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.UUID

@Controller
@RequestMapping("/lti")
class LtiController : BaseController() {
  @Autowired
  lateinit var ltiService: LtiService

  /**
   * Responsible for LTI Deep Linking requests
   * Shows a list of assignments that can be selected and linked in an LMS
   * @param cachedJwtId UUID of the cached incoming JWT
   */
  @GetMapping("/deep-link")
  fun listDeepLink(@RequestParam(name = "jwt") cachedJwtId: UUID, model: Model): String {
    model.addAttribute("assignments", assignmentService.findAllAssignments())
    model.addAttribute("jwtId", cachedJwtId)
    return "lti/deep-link-list"
  }

  /**
   * Handle the selected deep link
   * @param cachedJwtId UUID of the cached incoming JWT
   */
  @PostMapping("/deep-link")
  fun postDeepLink(
    @RequestParam(name = "jwt") cachedJwtId: UUID,
    @RequestParam selectedAssignmentId: UUID,
    model: Model
  ): String {
    val requestJwt = ltiService.findCachedJwtClaimsSet(cachedJwtId)
    val assignment = assignmentService.findAssignment(selectedAssignmentId)
    val responseJwt = ltiService.buildDeepLinkingResponse(requestJwt, assignment)
    ltiService.removeCachedJwtClaimSet(cachedJwtId)
    model.addAttribute(
        "redirect_url",
        requestJwt.getJSONObjectClaim("https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings")?.getAsString(
            "deep_link_return_url"
        )
    )
    model.addAttribute("fields", mapOf("JWT" to responseJwt.serialize()))
    return "lti/post-redirect"
  }

  @RequestMapping("/launch/{id}")
  @ResponseBody
  fun launchRequest(@PathVariable("id") assignmentId: UUID): String {
    return "TODO: Show instructions for assignment ${assignmentId}"
  }
}
