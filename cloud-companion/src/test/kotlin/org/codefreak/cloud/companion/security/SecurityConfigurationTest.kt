package org.codefreak.cloud.companion.security

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux

/**
 * This is a test token with the following claims:
 * {
 *   "iss": "test-iss",
 *   "sub": "user@domain.org",
 *   "aud": "test-aud",
 *   "exp": 9633917713 // somewhere in year 2275
 * }
 */
const val TEST_JWT =
  "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0ZXN0LWlzcyIsInN1YiI6InVzZXJAZG9tYWluLm9yZyIsImF1ZCI6InRlc3QtYXVkIiwiZXhwIjo5NjMzOTE3NzEzfQ.k0lUULSeacpsxlCoJip62ghkP4qAZJRjVM4TTjaZ__ku9NLRBfyKCDeVhw63jGsRlWTY3Qir0ALnOiWX4uebN9C4tY1oFJ3XkCfsxNEB5FhC4YIe99NP2UkXWHdexM7SpMaUVd9zYdDF9cynzHlpRDWPmfOfMGwGG749oo8IGMAV4cFgjT8JeTwGQZCQAMPJgzKy4iwfjgL7It1d85rx5VNsWFnstmnONfNon2F_9ad1XznuKvvWhSlkYOVS5Jg6VW5HIpvZfH8LeTHsovji6dGXGyu2iSnP92EahL8uQvJrmOKR4Bp8K1cAdpS4QFPUlUO5YJb1-Y1V0BKW2TO8tw"

@SpringBootTest(
  properties = [
    "spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:publickey.pem",
    "companion.jwt.claims.issuer=test-iss",
    "companion.jwt.claims.audience=test-aud"
  ],
  webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
internal class SecurityConfigurationTest {
  @Autowired
  lateinit var client: WebTestClient

  @Test
  fun `regular requests are unauthorized when no token is provided`() {
    client.get()
      .uri("/")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `valid JWT grants access to the companion`() {
    client.get()
      .uri("/")
      .header("Authorization", "Bearer $TEST_JWT")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `that GraphQL WS auth works`(@LocalServerPort localServerPort: Int, @Autowired objectMapper: ObjectMapper) {
    val ackReceived = AtomicBoolean(false)
    val closeStatus = AtomicReference<CloseStatus>()
    ReactorNettyWebSocketClient().execute(URI("http://localhost:$localServerPort/graphql")) { session ->
      session.send(
        Flux.just(
          session.textMessage(
            """
{
  "type": "connection_init",
  "payload": {
    "jwt": "$TEST_JWT"
  }
}
    """.trimIndent()
          )
        )
      )
        .thenMany(
          session.receive()
            .take(1)
            .map { objectMapper.readTree(it.payloadAsText) }
            .map {
              if (it.get("type").toString() == "\"connection_ack\"") {
                ackReceived.compareAndSet(false, true)
              }
              session.close()
            }
        )
        .then(session.closeStatus())
        .map { closeStatus.set(it) }
        .then()
    }.block()

    assertThat("Ack has not been received", ackReceived.get())
    assertThat(closeStatus.get().code, equalTo(1006))
  }

  @Test
  fun `that GraphQL WS connection is refused if no JWT is provided`(
    @LocalServerPort localServerPort: Int,
    @Autowired objectMapper: ObjectMapper
  ) {
    val ackReceived = AtomicBoolean(false)
    val closeStatus = AtomicReference<CloseStatus>()
    ReactorNettyWebSocketClient().execute(URI("http://localhost:$localServerPort/graphql")) { session ->
      session.send(Flux.just(session.textMessage("""{"type": "connection_init"}""")))
        .thenMany(
          session.receive()
            .take(1)
            .map { objectMapper.readTree(it.payloadAsText) }
            .map {
              if (it.get("type").toString() == "\"connection_ack\"") {
                ackReceived.compareAndSet(false, true)
              }
              session.close()
            }
        )
        .then(session.closeStatus())
        .map { closeStatus.set(it) }
        .then()
    }.block()

    assertThat("Ack has been received but was unexpected", !ackReceived.get())
    assertThat(closeStatus.get().code, equalTo(4401))
  }

  @Test
  fun `that GraphQL WS connection ack works if jwt is provided in header`(
    @LocalServerPort localServerPort: Int,
    @Autowired objectMapper: ObjectMapper
  ) {
    val ackReceived = AtomicBoolean(false)
    val closeStatus = AtomicReference<CloseStatus>()
    val headers = HttpHeaders()
    headers.setBearerAuth(TEST_JWT)
    ReactorNettyWebSocketClient().execute(URI("http://localhost:$localServerPort/graphql"), headers) { session ->
      session.send(Flux.just(session.textMessage("""{"type": "connection_init"}""")))
        .thenMany(
          session.receive()
            .take(1)
            .map { objectMapper.readTree(it.payloadAsText) }
            .map {
              if (it.get("type").toString() == "\"connection_ack\"") {
                ackReceived.compareAndSet(false, true)
              }
              session.close()
            }
        )
        .then(session.closeStatus())
        .map { closeStatus.set(it) }
        .then()
    }.block()

    assertThat("Ack has not been received", ackReceived.get())
    assertThat(closeStatus.get().code, equalTo(1006))
  }

  @Test
  fun `that GQL endpoint is not reachable via regular HTTP POST requests`() {
    client.post()
      .uri("/graphql")
      .contentType(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `that GQL endpoint is not reachable via regular HTTP GET requests`() {
    client.get()
      .uri("/graphql")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
