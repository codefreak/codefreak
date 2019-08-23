package de.code_freak.codefreak.auth

import com.nhaarman.mockitokotlin2.any
import de.code_freak.codefreak.SpringFrontendTest
import de.code_freak.codefreak.auth.lti.LtiAuthenticationFilter
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.mitre.jwt.signer.service.JWTSigningAndValidationService
import org.mitre.jwt.signer.service.impl.JWKSetCacheService
import org.mitre.openid.connect.client.service.ServerConfigurationService
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

class LtiAuthTest : SpringFrontendTest() {
  @Autowired
  lateinit var serverConfiguration: ServerConfigurationService

  @Autowired
  lateinit var ltiAuthenticationFilter: LtiAuthenticationFilter

  @Before
  fun before() {
    ltiAuthenticationFilter.restClient = Mockito.mock(RestTemplate::class.java)
    val mockValidator = Mockito.mock(JWTSigningAndValidationService::class.java)
    `when`(mockValidator.validateSignature(any())).thenReturn(true)
    val validationServiceMock = Mockito.mock(JWKSetCacheService::class.java)
    `when`(validationServiceMock.getValidator(any())).thenReturn(mockValidator)
    ltiAuthenticationFilter.validationServices = validationServiceMock
  }

  /**
   * Assert that any incoming authorization request is correctly initiated by redirecting to the LMS auth endpoint
   */
  @Test
  fun ltiLoginFlowInit() {
    val serverConfig = serverConfiguration.getServerConfiguration("https://lms.example.org")
    this.mockMvc.perform(
        post("/lti/login")
            .param("iss", serverConfig.issuer)
            .param("target_link_uri", "https://codefreak.example.org/lti/launch/123")
            .param("login_hint", "foo")
            .param("lti_message_hint", "bar")
    )
        .andExpect(status().is3xxRedirection)
        .andExpect(header().string("Location", Matchers.startsWith(serverConfig.authorizationEndpointUri)))
  }

  /**
   * Assert that the LtiResourceLinkRequest is handled correctly and redirects to the selected resource
   */
  @Test
  fun ltiResourceLinkRequest() {
    val serverConfig = serverConfiguration.getServerConfiguration("https://lms.example.org")
    `when`(
        ltiAuthenticationFilter.restClient
            .postForObject(any<String>(), any(), any<Class<Any>>())
    )
        .thenReturn(tokenRequestResponse)
    this.mockMvc.perform(
        post("/lti/login")
            .sessionAttr("state", "bar")
            .sessionAttr("nonce", "1a0be8f75fde2")
            .sessionAttr("issuer", serverConfig.issuer)
            .param("id_token", JWT_LtiResourceLinkRequest)
            .param("state", "bar")
    )
        .andExpect(status().is3xxRedirection)
        .andExpect(header().string("Location", Matchers.startsWith("https://codefreak.example.org/lti/launch/123")))
  }

  // use https://jwt.io/ to see the content of the JWTs
  val JWT_LtiResourceLinkRequest = """
     |eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImIwZDY4ZTAwZjk5ZTc0ODMxZjFhIn0.eyJub25jZSI6IjFhMGJlOGY3NWZkZTIiLCJpYX
     |QiOjE1NjY1NDcwNzYsImV4cCI6MTU2NjU0NzEzNiwiaXNzIjoiaHR0cHM6Ly9sbXMuZXhhbXBsZS5vcmciLCJhdWQiOiJZRGJMOTh4ZjlLRFJ0ZXki
     |LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9kZXBsb3ltZW50X2lkIjoiMyIsImh0dHBzOi8vcHVybC5pbXNnbG9iYW
     |wub3JnL3NwZWMvbHRpL2NsYWltL3RhcmdldF9saW5rX3VyaSI6Imh0dHBzOi8vY29kZWZyZWFrLmV4YW1wbGUub3JnL2x0aS9sYXVuY2gvMTIzIiwi
     |c3ViIjoiMiIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2xpcyI6eyJwZXJzb25fc291cmNlZGlkIjoiIiwiY291cn
     |NlX3NlY3Rpb25fc291cmNlZGlkIjoiIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL3JvbGVzIjpbImh0dHA6Ly9w
     |dXJsLmltc2dsb2JhbC5vcmcvdm9jYWIvbGlzL3YyL2luc3RpdHV0aW9uL3BlcnNvbiNBZG1pbmlzdHJhdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYm
     |FsLm9yZy92b2NhYi9saXMvdjIvbWVtYmVyc2hpcCNJbnN0cnVjdG9yIiwiaHR0cDovL3B1cmwuaW1zZ2xvYmFsLm9yZy92b2NhYi9saXMvdjIvc3lz
     |dGVtL3BlcnNvbiNBZG1pbmlzdHJhdG9yIl0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2NvbnRleHQiOnsiaWQiOi
     |IyIiwibGFiZWwiOiJDb3Vyc2UgIzEiLCJ0aXRsZSI6IkNvdXJzZSAjMSIsInR5cGUiOlsiQ291cnNlU2VjdGlvbiJdfSwiaHR0cHM6Ly9wdXJsLmlt
     |c2dsb2JhbC5vcmcvc3BlYy9sdGkvY2xhaW0vcmVzb3VyY2VfbGluayI6eyJ0aXRsZSI6IiIsImlkIjoiMyJ9LCJnaXZlbl9uYW1lIjoiQWRtaW4iLC
     |JmYW1pbHlfbmFtZSI6IlVzZXIiLCJuYW1lIjoiQWRtaW4gVXNlciIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL2V4
     |dCI6eyJ1c2VyX3VzZXJuYW1lIjoiQWRtaW4iLCJsbXMiOiJtb29kbGUtMiJ9LCJlbWFpbCI6InVzZXJAZXhhbXBsZS5vcmciLCJodHRwczovL3B1cm
     |wuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS9sYXVuY2hfcHJlc2VudGF0aW9uIjp7ImxvY2FsZSI6ImVuIiwiZG9jdW1lbnRfdGFyZ2V0Ijoi
     |aWZyYW1lIiwicmV0dXJuX3VybCI6Imh0dHBzOi8vbG1zLmV4YW1wbGUub3JnL21vZC9sdGkvcmV0dXJuLnBocD9jb3Vyc2U9MiZsYXVuY2hfY29udG
     |FpbmVyPTMmaW5zdGFuY2VpZD0zJnNlc3NrZXk9emV6OWN3UjlMOCJ9LCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS9jbGFpbS90
     |b29sX3BsYXRmb3JtIjp7ImZhbWlseV9jb2RlIjoibW9vZGxlIiwidmVyc2lvbiI6IjIwMTkwNTIwMDEiLCJndWlkIjoibG9jYWxob3N0IiwibmFtZS
     |I6IlwiTmV3IFNpdGVcIiIsImRlc2NyaXB0aW9uIjoiXCJOZXcgU2l0ZVwiIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2Ns
     |YWltL3ZlcnNpb24iOiIxLjMuMCIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpL2NsYWltL21lc3NhZ2VfdHlwZSI6Ikx0aVJlc2
     |91cmNlTGlua1JlcXVlc3QiLCJodHRwczovL3B1cmwuaW1zZ2xvYmFsLm9yZy9zcGVjL2x0aS1hZ3MvY2xhaW0vZW5kcG9pbnQiOnsic2NvcGUiOlsi
     |aHR0cHM6Ly9wdXJsLmltc2dsb2JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL2xpbmVpdGVtLnJlYWRvbmx5IiwiaHR0cHM6Ly9wdXJsLmltc2dsb2
     |JhbC5vcmcvc3BlYy9sdGktYWdzL3Njb3BlL3Jlc3VsdC5yZWFkb25seSIsImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLWFncy9z
     |Y29wZS9zY29yZSJdLCJsaW5laXRlbXMiOiJodHRwOi8vbG9jYWxob3N0OjgwOTkvbW9kL2x0aS9zZXJ2aWNlcy5waHAvMi9saW5laXRlbXM_dHlwZV
     |9pZD0zIn0sImh0dHBzOi8vcHVybC5pbXNnbG9iYWwub3JnL3NwZWMvbHRpLW5ycHMvY2xhaW0vbmFtZXNyb2xlc2VydmljZSI6eyJjb250ZXh0X21l
     |bWJlcnNoaXBzX3VybCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA5OS9tb2QvbHRpL3NlcnZpY2VzLnBocC9Db3Vyc2VTZWN0aW9uLzIvYmluZGluZ3MvMy
     |9tZW1iZXJzaGlwcyIsInNlcnZpY2VfdmVyc2lvbnMiOlsiMS4wIiwiMi4wIl19fQ.bFb2wi_TWi9xIZVMisG6iiFWmCilyla1cUXht4rN1vIfscyys
     |ufvzwSpPNL0__UZ98H6Zgw6Jb8FxaX2zZirLZQj1nQc1hKJpAR4Xk3iebtpIXDg6o6i_T4yOMtWu5RKPe-nVsK4X-d0UKev7JJTYNq8cVHpN08KT-Q
     |U8dpPgm8cfWaVPgbMDpxWDbsxrvO7zphIFJNaEVy-X-13Dw_xdphagDw0UxoiEAlYZcZz7lWYZcuPqCKJhdmsnnbl_jhLmVkdEjq5T4X2FZEWGqmvo
     |CL1BLTYals4_ODHuoGK291W2QQDKgT3ksYH1QkgYlq3pD6OLR9fuA3fSRrpDy2mUg
    """.trimMargin()

  val tokenRequestResponse = """
    {
      "access_token": "foo",
      "token_type": "Bearer",
      "expires_in": "123",
      "scope": "foo"
    }
  """.trimIndent()
}
