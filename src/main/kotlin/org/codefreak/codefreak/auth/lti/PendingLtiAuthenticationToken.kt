package org.codefreak.codefreak.auth.lti

import com.nimbusds.jwt.JWT
import org.mitre.openid.connect.config.ServerConfiguration
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken

class PendingLtiAuthenticationToken(
  subject: String,
  issuer: String,
  serverConfiguration: ServerConfiguration,
  idToken: JWT,
  accessToken: String,
  refreshToken: String?
) : PendingOIDCAuthenticationToken(subject, issuer, serverConfiguration, idToken, accessToken, refreshToken)
