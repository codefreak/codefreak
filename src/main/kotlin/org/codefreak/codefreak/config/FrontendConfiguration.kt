package org.codefreak.codefreak.config

import org.codefreak.codefreak.frontend.ShortUuidConverter
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
