package org.codefreak.cloud.companion.security

import org.codefreak.cloud.companion.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ConfigurationCondition
import org.springframework.graphql.web.WebSocketInterceptor
import org.springframework.http.HttpMethod.GET
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers

/**
 * Security is enabled when either a JWK url is set or a static public key is provided via one of the
 * following properties:
 * - spring.security.oauth2.resourceserver.jwt.jwk-set-uri
 * - spring.security.oauth2.resourceserver.jwt.public-key-location
 *
 * @see org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerJwkConfiguration
 */
@EnableWebFluxSecurity
@Configuration
class SecurityConfiguration {
  private val log = logger()

  @Conditional(JwtAuthEnabled::class)
  class JwtSecurityConfig {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
      return http {
        httpBasic { disable() }
        formLogin { disable() }
        logout { disable() }
        csrf { disable() }
        authorizeExchange {
          authorize("/actuator/**", permitAll)
          // GQL has its own authentication when upgrading to a websocket connection via GET /graphql.
          // Regular authentication should kick in if a POST request with GQL payload is sent to /graphql.
          authorize(AndServerWebExchangeMatcher(isWebsocketUpgradeRequest(), pathMatchers(GET, "/graphql")), permitAll)
          // Process IO has its own authentication when upgrading to a websocket connection via GET /process/{id}.
          authorize(
            AndServerWebExchangeMatcher(isWebsocketUpgradeRequest(), pathMatchers(GET, "/process/**")),
            permitAll
          )
          authorize(anyExchange, authenticated)
        }
        oauth2ResourceServer {
          jwt {
            // Config is passed in via properties
          }
        }
      }
    }

    private fun isWebsocketUpgradeRequest(): ServerWebExchangeMatcher {
      return ServerWebExchangeMatcher {
        if ("WebSocket".equals(it.request.headers.upgrade, ignoreCase = true)) {
          ServerWebExchangeMatcher.MatchResult.match()
        } else {
          ServerWebExchangeMatcher.MatchResult.notMatch()
        }
      }
    }

    @Bean
    fun overrideJwtValidator(tokenValidator: OAuth2TokenValidator<Jwt>): BeanPostProcessor {
      return object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
          if (bean is NimbusReactiveJwtDecoder) {
            bean.setJwtValidator(tokenValidator)
          }
          return bean
        }
      }
    }

    @Bean
    fun jwtTokenValidator(
      @Value("\${companion.jwt.claims.issuer:}") issuer: String,
      @Value("\${companion.jwt.claims.audience:}") audience: String
    ): OAuth2TokenValidator<Jwt> {
      val validators = mutableListOf<OAuth2TokenValidator<Jwt>>()
      validators.add(JwtTimestampValidator())
      if (issuer.isNotEmpty()) {
        println("Expecting issuer with value $issuer")
        validators.add(JwtIssuerValidator(issuer))
      }
      if (audience.isNotBlank()) {
        println("Expecting audience with value $audience")
        validators.add(jwtClaimValueValidator(JwtClaimNames.AUD, audience))
      }
      return DelegatingOAuth2TokenValidator(validators)
    }

    @Bean
    fun jwtAuthService(jwtDecoder: ReactiveJwtDecoder): JwtWebsocketAuthenticationService {
      return JwtWebsocketAuthenticationService(jwtDecoder)
    }

    /**
     * Add authentication to GraphQL websockets. Browsers do not support
     * providing custom Headers when initializing a websocket connection.
     * This is why there is a dedicated ConnectionInit message in the
     * graphql-ws protocol which allows authentication before performing
     * any requests.
     */
    @Bean
    fun graphqlWebsocketAuthInterceptor(jwtWebsocketAuthenticationService: JwtWebsocketAuthenticationService): WebSocketInterceptor {
      return ConnectionInitAuthInterceptor(jwtWebsocketAuthenticationService)
    }

    private fun jwtClaimValueValidator(claimName: String, expectedValue: String): JwtClaimValidator<Any?> {
      val testClaimValue = { actualValue: Any? ->
        if (actualValue is Iterable<*>) {
          actualValue.contains(expectedValue)
        } else {
          actualValue == expectedValue
        }
      }
      return JwtClaimValidator(claimName, testClaimValue)
    }
  }

  /**
   * There is no user storage. Incoming requests are authenticated stateless
   * via JWT. By default, Spring creates an in-memory user service with a single
   * user. This Bean is here to prevent this behaviour.
   */
  @Bean
  fun userDetailsService(): ReactiveUserDetailsService {
    return MapReactiveUserDetailsService(emptyMap())
  }

  /**
   * Default web security that allows all requests without auth.
   * This should never be used in production but allows easier testing.
   */
  @Bean
  @ConditionalOnMissingBean(SecurityWebFilterChain::class)
  fun defaultSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    log.warn(
      """


    ${"!".repeat(60)}
    Companion is running without authentication!
    This should only ever be used for testing!
    ${"!".repeat(60)}

    """.trimIndent()
    )
    return http {
      httpBasic { disable() }
      formLogin { disable() }
      logout { disable() }
      csrf { disable() }
      authorizeExchange {
        authorize(anyExchange, permitAll)
      }
    }
  }

  /**
   * Condition that checks if one of the following properties is set:
   * * spring.security.oauth2.resourceserver.jwt.jwk-set-uri
   * * spring.security.oauth2.resourceserver.jwt.public-key-location
   */
  internal class JwtAuthEnabled : AnyNestedCondition(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION) {
    @ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    class JwkSetUriAvailable

    @ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.public-key-location")
    class PublicKeyLocationAvailable
  }
}
