package org.codefreak.codefreak.config

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CompanionSecurityConfiguration {
  @Bean
  fun keyPair(): KeyPair {
    return KeyPairGenerator.getInstance("RSA").also {
      it.initialize(2048, SecureRandom())
    }.genKeyPair()
  }
}
