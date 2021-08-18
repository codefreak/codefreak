package org.codefreak.codefreak.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
  ): ObjectMapper = builder.createXmlMapper(false)
      .build<ObjectMapper>()
      .registerKotlinModule()

  @Bean("yamlObjectMapper")
  fun yamlObjectMapper(
    builder: Jackson2ObjectMapperBuilder
  ): ObjectMapper {
    val yamlFactory = YAMLFactoryBuilder(YAMLFactory())
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // do not use quotes when not needed
        .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS) // but always quote numbers if they are stored as string
        .disable(YAMLGenerator.Feature.SPLIT_LINES) // do not split long lines as this will cause hard to read strings
        .build()
    return ObjectMapper(yamlFactory).apply {
      registerKotlinModule()
      setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
  }

  @Bean("xmlObjectMapper")
  fun xmlObjectMapper(
    builder: Jackson2ObjectMapperBuilder
  ) = builder.createXmlMapper(true)
      .build<ObjectMapper>()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerKotlinModule()
}
