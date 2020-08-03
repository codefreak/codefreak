package org.codefreak.codefreak.auth.lti

import com.nimbusds.jwt.JWTClaimsSet
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codefreak.codefreak.service.LtiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.web.util.UriComponentsBuilder

class LtiAuthenticationSuccessHandler(defaultTargetUrl: String) :
    SimpleUrlAuthenticationSuccessHandler(defaultTargetUrl) {

  companion object {
    const val MESSAGE_TYPE_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/message_type"
    const val MESSAGE_TYPE_DEEP_LINK_REQUEST = "LtiDeepLinkingRequest"
    const val MESSAGE_TYPE_RESOURCE_LINK = "LtiResourceLinkRequest"
    const val TARGET_LINK_URI_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/target_link_uri"
  }

  var deepLinkRedirectUrl: String = "/lti/deep-link"

  @Autowired
  lateinit var ltiService: LtiService

  override fun onAuthenticationSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication?
  ) {
    if (authentication !is LtiAuthenticationToken) {
      return super.onAuthenticationSuccess(request, response, authentication)
    }

    return when (authentication.claims.getStringClaim(MESSAGE_TYPE_CLAIM)) {
      MESSAGE_TYPE_DEEP_LINK_REQUEST -> redirectDeepLinkRequest(response, authentication)
      MESSAGE_TYPE_RESOURCE_LINK -> redirectResourceLinkRequest(response, authentication)
      else -> super.onAuthenticationSuccess(request, response, authentication)
    }
  }

  /**
   * Redirect a LtiResourceLinkRequest to the correct resource that is specified in a claim
   */
  private fun redirectResourceLinkRequest(response: HttpServletResponse, authentication: LtiAuthenticationToken) {
    val uuid = storeClaimSet(authentication.claims)
    val redirectUri = authentication.claims.getStringClaim(TARGET_LINK_URI_CLAIM)
    val uri = UriComponentsBuilder.fromUriString(redirectUri).queryParam("jwt", uuid.toString())
    return response.sendRedirect(uri.toUriString())
  }

  private fun storeClaimSet(claims: JWTClaimsSet): UUID {
    return ltiService.cacheJwtClaimsSet(claims)
  }

  /**
   * Redirect to the LtiDeepLinkingRequest with all parameters from the deep link settings claim as query parameters
   */
  private fun redirectDeepLinkRequest(response: HttpServletResponse, authentication: LtiAuthenticationToken) {
    val uuid = storeClaimSet(authentication.claims)
    val uriBuilder = UriComponentsBuilder.fromPath(this.deepLinkRedirectUrl).queryParam("jwt", uuid.toString())
    return response.sendRedirect(uriBuilder.toUriString())
  }
}
