package org.codefreak.codefreak.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter

@Configuration
class GraphQLConfiguration {

  /**
   * Enable JPA lazy loading for GraphQL DTOs
   */
  @Bean
  fun openEntityManagerInViewFilter() = FilterRegistrationBean<OpenEntityManagerInViewFilter>().apply {
    filter = OpenEntityManagerInViewFilter()
    addUrlPatterns("/graphql")
  }
}
