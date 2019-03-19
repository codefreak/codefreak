package de.code_freak.codefreak.config

import nz.net.ultraq.thymeleaf.LayoutDialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FrontendConfiguration {

  @Bean
  fun layoutDialect(): LayoutDialect {
    return LayoutDialect()
  }
}
