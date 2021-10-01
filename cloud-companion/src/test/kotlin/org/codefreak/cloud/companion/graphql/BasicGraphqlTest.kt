package org.codefreak.cloud.companion.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.WebGraphQlTester
import org.springframework.graphql.web.WebGraphQlHandler
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
abstract class BasicGraphqlTest {
  protected lateinit var graphQlTester: GraphQlTester

  @BeforeEach
  fun setUp(@Autowired handler: WebGraphQlHandler, @Autowired objectMapper: ObjectMapper) {
    graphQlTester = WebGraphQlTester.builder(handler)
      .jsonPathConfig(
        Configuration.builder()
          .jsonProvider(JacksonJsonProvider(objectMapper))
          .mappingProvider(JacksonMappingProvider(objectMapper))
          .build()
      ).build()
  }
}
