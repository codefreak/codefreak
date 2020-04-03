package org.codefreak.codefreak.auth.lti

import org.mitre.oauth2.model.RegisteredClient
import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder
import org.mitre.openid.connect.config.ServerConfiguration

class IdCodeAuthRequestBuilder : PlainAuthRequestUrlBuilder() {
  override fun buildAuthRequestUrl(
    serverConfig: ServerConfiguration?,
    clientConfig: RegisteredClient?,
    redirectUri: String?,
    nonce: String?,
    state: String?,
    options: MutableMap<String, String>?,
    loginHint: String?
  ): String {
    return super.buildAuthRequestUrl(serverConfig, clientConfig, redirectUri, nonce, state, options, loginHint).run {
      // parent sadly returns a string and not a UriBuilder instance
      this.replace("response_type=code", "response_type=id_token")
    }
  }
}
