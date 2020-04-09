package org.codefreak.codefreak.graphql

import graphql.schema.GraphQLScalarType
import graphql.servlet.core.ApolloScalars
import org.apache.catalina.core.ApplicationPart
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClassifier

object ScalarTypes {
  private val scalars = mapOf(
      UUID::class to GraphQLScalarType.Builder()
          .name("ID")
          .description("UUID")
          .coercing(UuidConverter())
          .build(),
      Instant::class to GraphQLScalarType.Builder()
          .name("DateTime")
          .description("UTC date and time in ISO-8601 format")
          .coercing(DateTimeConverter())
          .build(),
      ApplicationPart::class to ApolloScalars.Upload
  )
  fun get(classifier: KClassifier) = scalars[classifier]
}
