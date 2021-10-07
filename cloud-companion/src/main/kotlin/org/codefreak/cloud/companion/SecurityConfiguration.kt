package org.codefreak.cloud.companion

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Security is enabled when a jwk-set-uri is provided to the companion
 */
@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
@EnableWebFluxSecurity
@Configuration
class SecurityConfiguration {

  @Bean
  fun springSecurityFilterChain2(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http {
      csrf {
        disable()
      }
      authorizeExchange {
        authorize("/actuator/**", permitAll)
        authorize(anyExchange, authenticated)
      }
      oauth2ResourceServer {
        jwt {
          // publicKey
          // jwtDecoder = NimbusReactiveJwtDecoder.withSecretKey(
          //  SecretKeySpec("0".repeat(32).toByteArray(UTF_8), "HmacSHA256")
          // ).build()
        }
      }
    }
  }

  /*
  @Bean
  fun customizeJwtDecoder(): BeanPostProcessor {
    return object : BeanPostProcessor {
      override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is NimbusReactiveJwtDecoder) {
          bean.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://theissuer.local"))
        }
        return bean
      }
    }
  }
  */
}
