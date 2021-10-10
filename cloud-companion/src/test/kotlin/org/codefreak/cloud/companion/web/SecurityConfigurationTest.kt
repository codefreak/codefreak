package org.codefreak.cloud.companion.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

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
  ]
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
internal class SecurityConfigurationTest {
  @Autowired
  lateinit var client: WebTestClient

  @Test
  fun testUnauthorizedIfNoJwtIsPassed() {
    client.get()
      .uri("/not-there")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun testAuthorizedIfValidJwtIsPassed() {
    client.get()
      .uri("/not-there")
      .header("Authorization", "Bearer $TEST_JWT")
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
