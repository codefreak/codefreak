package de.code_freak.codefreak.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver

@Configuration
class LatexConfiguration {
  @Qualifier("latexTemplate")
  @Bean
  fun latexTemplateEngine(): TemplateEngine {
    val templateEngine = SpringTemplateEngine()
    templateEngine.addTemplateResolver(latexTemplateResolver())
    return templateEngine
  }

  private fun latexTemplateResolver(): ITemplateResolver {
    val templateResolver = ClassLoaderTemplateResolver()
    templateResolver.order = 1
    templateResolver.prefix = "latex/"
    templateResolver.suffix = ".tex"
    templateResolver.templateMode = TemplateMode.TEXT
    templateResolver.isCacheable = true
    return templateResolver
  }
}
