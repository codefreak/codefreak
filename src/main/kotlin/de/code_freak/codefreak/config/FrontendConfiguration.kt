package de.code_freak.codefreak.config

import de.code_freak.codefreak.frontend.ShortUuidConverter
import nz.net.ultraq.thymeleaf.LayoutDialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class FrontendConfiguration : WebMvcConfigurer {

  @Bean
  fun layoutDialect(): LayoutDialect {
    return LayoutDialect()
  }

  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ShortUuidConverter())
  }
}
