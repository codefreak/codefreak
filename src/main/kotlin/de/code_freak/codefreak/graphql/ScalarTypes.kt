package de.code_freak.codefreak.graphql

import graphql.schema.GraphQLScalarType
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClassifier

object ScalarTypes {
  private val scalars = mapOf(
      UUID::class to GraphQLScalarType.Builder()
          .name("ID")
          .description("ID to java.util.UUID")
          .coercing(UuidConverter())
          .build(),
      Instant::class to GraphQLScalarType.Builder()
          .name("DateTime")
          .description("DateTime to java.time.Instant")
          .coercing(DateTimeConverter())
          .build()
  )
  fun get(classifier: KClassifier) = scalars[classifier]
}
