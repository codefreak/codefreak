package de.code_freak.codefreak.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter
import javax.servlet.Filter

@Configuration
class GraphQLConfiguration {

  /**
   * Enable JPA lazy loading for GraphQL DTOs
   */
  @Bean
  fun openFilter(): Filter {
    return OpenEntityManagerInViewFilter()
  }
}
