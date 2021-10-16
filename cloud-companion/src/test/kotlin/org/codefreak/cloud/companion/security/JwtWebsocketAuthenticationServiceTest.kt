package org.codefreak.cloud.companion.security

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
internal class JwtWebsocketAuthenticationServiceTest {
  @Mock
  private lateinit var jwtDecoder: ReactiveJwtDecoder

  @InjectMocks
  private lateinit var jwtWebsocketAuthenticationService: JwtWebsocketAuthenticationService

  @Test
  fun `auth fails if null payload is given`() {
    val handshakeInfo = mock(HandshakeInfo::class.java)
    `when`(handshakeInfo.principal).thenReturn(Mono.just(mock(AnonymousAuthenticationToken::class.java)))
    val session = mock(WebSocketSession::class.java)
    `when`(session.handshakeInfo).thenReturn(handshakeInfo)
    StepVerifier.create(
      jwtWebsocketAuthenticationService.authenticateWebsocketSession(session, null)
    )
      .verifyError(JwtWebsocketAuthenticationService.JwtAuthenticationException::class.java)
  }

  @Test
  fun `auth fails if empty jwt payload is given`() {
    val handshakeInfo = mock(HandshakeInfo::class.java)
    `when`(handshakeInfo.principal).thenReturn(Mono.just(mock(AnonymousAuthenticationToken::class.java)))
    val session = mock(WebSocketSession::class.java)
    `when`(session.handshakeInfo).thenReturn(handshakeInfo)
    StepVerifier.create(
      jwtWebsocketAuthenticationService.authenticateWebsocketSession(session, emptyMap())
    )
      .verifyError(JwtWebsocketAuthenticationService.JwtAuthenticationException::class.java)
  }

  @Test
  fun `auth is successful if jwt is in session info`() {
    val jwt = JwtAuthenticationToken(Jwt.withTokenValue("ey").header("alg", "foo").claim("sub", "foo").build())
    val handshakeInfo = mock(HandshakeInfo::class.java)
    `when`(handshakeInfo.principal).thenReturn(Mono.just(jwt))
    val session = mock(WebSocketSession::class.java)
    `when`(session.handshakeInfo).thenReturn(handshakeInfo)
    StepVerifier.create(
      jwtWebsocketAuthenticationService.authenticateWebsocketSession(session, emptyMap())
    )
      .assertNext { assertThat(it, hasEntry("sub", "foo")) }
      .verifyComplete()
  }

  @Test
  fun `auth is successful if jwt is in payload`() {
    val handshakeInfo = mock(HandshakeInfo::class.java)
    `when`(handshakeInfo.principal).thenReturn(Mono.just(mock(AnonymousAuthenticationToken::class.java)))
    val session = mock(WebSocketSession::class.java)
    `when`(session.handshakeInfo).thenReturn(handshakeInfo)

    `when`(jwtDecoder.decode("stubbed")).thenReturn(
      Mono.just(
        Jwt.withTokenValue("ey").header("alg", "foo").claim("sub", "foo").build()
      )
    )

    StepVerifier.create(
      jwtWebsocketAuthenticationService.authenticateWebsocketSession(session, mapOf("jwt" to "stubbed"))
    )
      .assertNext { assertThat(it, hasEntry("sub", "foo")) }
      .verifyComplete()
  }

  @Test
  fun `auth prefers jwt from session`() {
    val jwt = JwtAuthenticationToken(Jwt.withTokenValue("ey").header("alg", "foo").claim("sub", "foo").build())
    val handshakeInfo = mock(HandshakeInfo::class.java)
    `when`(handshakeInfo.principal).thenReturn(Mono.just(jwt))
    val session = mock(WebSocketSession::class.java)
    `when`(session.handshakeInfo).thenReturn(handshakeInfo)

    StepVerifier.create(
      jwtWebsocketAuthenticationService.authenticateWebsocketSession(session, mapOf("jwt" to "stubbed"))
    )
      .assertNext { assertThat(it, hasEntry("sub", "foo")) }
      .verifyComplete()
    verifyNoInteractions(jwtDecoder)
  }
}
