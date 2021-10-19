package org.codefreak.codefreak.service.workspace

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Spring configuration that will be passed to the companion
 */
@JsonAutoDetect(fieldVisibility = ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CompanionSpringConfig(
  @JsonProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
  private val jwkSetUrl: String?,
  @JsonProperty("companion.jwt.claims.issuer")
  private val jwtClaimIssuer: String?,
  @JsonProperty("companion.jwt.claims.audience")
  private val jwtClaimAudience: String?,
  @JsonProperty("server.tomcat.max-threads")
  private val maxTomcatThreads: String = "5",
  @JsonProperty("server.tomcat.max-connections")
  private val maxTomcatConnections: String = "20"
)
