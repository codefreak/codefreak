package de.code_freak.codefreak.graphql

import com.expediagroup.graphql.hooks.SchemaGeneratorHooks
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KType

@Component
class CustomSchemaGeneratorHooksImpl : SchemaGeneratorHooks {
  override fun willGenerateGraphQLType(type: KType): GraphQLType? {
    return when (type.classifier) {
      UUID::class -> GraphQLScalarType.Builder()
          .name("ID")
          .description("ID to java.util.UUID")
          .coercing(UuidConverter())
          .build()
      Instant::class -> GraphQLScalarType.Builder()
          .name("DateTime")
          .description("DateTime to java.time.Instant")
          .coercing(DateTimeConverter())
          .build()
      else -> null
    }
  }
}
