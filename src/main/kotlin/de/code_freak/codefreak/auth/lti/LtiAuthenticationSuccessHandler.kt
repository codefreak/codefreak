package de.code_freak.codefreak.auth.lti

import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.web.util.UriComponentsBuilder
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LtiAuthenticationSuccessHandler(defaultTargetUrl: String) :
    SimpleUrlAuthenticationSuccessHandler(defaultTargetUrl) {

  companion object {
    const val MESSAGE_TYPE_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/message_type"
    const val MESSAGE_TYPE_DEEP_LINK_REQUEST = "LtiDeepLinkingRequest"
    const val MESSAGE_TYPE_RESOURCE_LINK = "LtiResourceLinkRequest"
    const val DEEP_LINK_SETTINGS_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings"
    const val TARGET_LINK_URI_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/target_link_uri"
  }

  var deepLinkRedirectUrl: String = "/lti/deep-link"

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
    val redirectUri = authentication.claims.getStringClaim(TARGET_LINK_URI_CLAIM)
    return response.sendRedirect(redirectUri)
  }

  /**
   * Redirect to the LtiDeepLinkingRequest with all parameters from the deep link settings claim as query parameters
   */
  private fun redirectDeepLinkRequest(response: HttpServletResponse, authentication: LtiAuthenticationToken) {
    val settings = authentication.claims.getJSONObjectClaim(DEEP_LINK_SETTINGS_CLAIM)
    val uriBuilder = UriComponentsBuilder.fromPath(this.deepLinkRedirectUrl)
    for ((key, value) in settings.entries) {
      if (value is Iterable<*>) {
        value.forEach { itemValue -> uriBuilder.queryParam(key, itemValue) }
      } else {
        uriBuilder.queryParam(key, value)
      }
    }
    return response.sendRedirect(uriBuilder.toUriString())
  }
}
