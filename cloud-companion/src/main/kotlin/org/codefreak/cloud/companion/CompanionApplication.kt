package org.codefreak.cloud.companion

import org.apache.tika.Tika
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.graphql.boot.GraphQlSourceBuilderCustomizer
import org.springframework.graphql.data.GraphQlRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.GraphQlController
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.nativex.hint.AccessBits
import org.springframework.nativex.hint.NativeHint
import org.springframework.nativex.hint.ResourceHint
import org.springframework.nativex.hint.TypeHint

@NativeHint(
  resources = [
    ResourceHint(
      patterns = [
        "org/apache/tika/mime/tika-mimetypes.xml",
        "graphql/schema.graphqls",
        "com/sun/jna/linux-x86-64/libjnidispatch.so"
      ]
    )
  ],
  types = [
    TypeHint(
      types = [
        Argument::class,
        BatchMapping::class,
        GraphQlController::class,
        MutationMapping::class,
        QueryMapping::class,
        SchemaMapping::class,
        SubscriptionMapping::class,
        GraphQlRepository::class
      ],
      access = AccessBits.FULL_REFLECTION
    ),
    TypeHint(
      typeNames = [
        "org.apache.log4j.Category",
        "org.apache.log4j.Logger",
        "org.apache.log4j.helpers.Loader"
      ]
    )
  ]
)
@SpringBootApplication
@EnableConfigurationProperties(CompanionConfig::class)
class CompanionApplication {
  @Bean
  fun tika(): Tika = Tika()

  /**
   * This is needed for spring-native because the classpath pattern-lookup does not work
   * when running as native image. Additionally, the schema-location is set empty via application.yml.
   */
  @Bean
  fun fixedGqlSchemaLocation(): GraphQlSourceBuilderCustomizer {
    return GraphQlSourceBuilderCustomizer {
      it.schemaResources(ClassPathResource("graphql/schema.graphqls"))
    }
  }
}

fun main(args: Array<String>) {
  runApplication<CompanionApplication>(*args)
}
