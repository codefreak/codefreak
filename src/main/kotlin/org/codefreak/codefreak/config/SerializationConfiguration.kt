package org.codefreak.codefreak.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class SerializationConfiguration {
  @Bean
  @Primary
  fun defaultObjectMapper(
      builder: Jackson2ObjectMapperBuilder
  ): ObjectMapper = builder.createXmlMapper(false).build()

  @Bean("yamlObjectMapper")
  fun yamlObjectMapper(
      builder: Jackson2ObjectMapperBuilder
  ): ObjectMapper {
    val yamlFactory = YAMLFactoryBuilder(YAMLFactory())
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .build()
    return ObjectMapper(yamlFactory).apply {
      setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
  }
}
