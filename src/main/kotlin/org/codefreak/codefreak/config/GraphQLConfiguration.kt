package org.codefreak.codefreak.config

import com.expediagroup.graphql.execution.KotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.execution.SimpleKotlinDataFetcherFactoryProvider
import org.codefreak.codefreak.graphql.ShortCircuitObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
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

  /**
   * Do not introduce as "ObjectMapper" because this would replace the global jackson object mapper
   */
  @Bean("shortCutObjectMapper")
  fun shortCutJacksonObjectMapper(
    builder: Jackson2ObjectMapperBuilder
  ): ShortCircuitObjectMapper = ShortCircuitObjectMapper().also { builder.configure(it) }

  @Bean
  fun dataFetcherProvider(
    @Qualifier("shortCutObjectMapper") objectMapper: ShortCircuitObjectMapper
  ): KotlinDataFetcherFactoryProvider = SimpleKotlinDataFetcherFactoryProvider(objectMapper)
}
