package org.codefreak.codefreak.graphql.api

import com.expediagroup.graphql.spring.operations.Mutation
import java.util.UUID
import org.codefreak.codefreak.auth.Authority
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.graphql.BaseResolver
import org.codefreak.codefreak.service.AssignmentService
import org.codefreak.codefreak.service.LtiService
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

class LtiDeepLinkResponse(val signedJwt: String, val redirectUrl: String)

@Component
class LtiMutation : BaseResolver(), Mutation {

  @Throws(IllegalStateException::class)
  private fun requireLtiEnabled() = context {
    if (!serviceAccess.getService(AppConfiguration::class).lti.enabled) {
      throw IllegalStateException("LTI support is not enabled for this Code FREAK instance")
    }
  }

  @Transactional
  @Secured(Authority.ROLE_TEACHER)
  fun ltiCreateDeepLinkResponse(assignmentId: UUID, jwtId: UUID): LtiDeepLinkResponse {
    requireLtiEnabled()
    return context {
      val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(assignmentId)
      val ltiService = serviceAccess.getService(LtiService::class)
      val requestJwt = ltiService.findCachedJwtClaimsSet(jwtId)
      val launchUrl = ServletUriComponentsBuilder.fromCurrentRequestUri()
          .replacePath("/lti/launch/" + assignment.id)
          .toUriString()
      val responseJwt = ltiService.buildDeepLinkingResponse(requestJwt, url = launchUrl, title = assignment.title)
      ltiService.removeCachedJwtClaimSet(jwtId)
      val redirectUrl = requestJwt.getJSONObjectClaim("https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings")?.getAsString(
          "deep_link_return_url"
      )
          ?: throw IllegalStateException("No 'deep_link_return_url' found in 'deep_linking_settings'")

      LtiDeepLinkResponse(responseJwt.serialize(), redirectUrl)
    }
  }
}
