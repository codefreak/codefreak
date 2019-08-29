package de.code_freak.codefreak.config

import de.code_freak.codefreak.frontend.ShortUuidConverter
import nz.net.ultraq.thymeleaf.LayoutDialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.format.FormatterRegistry
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.Executors

@Configuration
class FrontendConfiguration : WebMvcConfigurer {

  @Bean
  fun layoutDialect(): LayoutDialect {
    return LayoutDialect()
  }

  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ShortUuidConverter())
  }

  override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
    configurer.setDefaultTimeout(-1)
    configurer.setTaskExecutor(asyncTaskExecutor())
  }

  @Bean
  fun asyncTaskExecutor(): AsyncTaskExecutor {
    return ConcurrentTaskExecutor(Executors.newFixedThreadPool(5))
  }
}
