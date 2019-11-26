package de.code_freak.codefreak.graphql

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.time.Instant
import java.time.format.DateTimeParseException

class DateTimeConverter : Coercing<Instant, Instant> {

  private fun parseLocalDateTime(input: Any?): Instant? {
    if (input is Instant) return input
    if (input !is String || input.isEmpty()) return null
    return try {
      Instant.parse(input)
    } catch (e: DateTimeParseException) {
      null
    }
  }

  override fun parseValue(input: Any?): Instant {
    return parseLocalDateTime(input) ?: throw CoercingParseValueException(
        "Expected type 'Instant' but was '${input?.javaClass?.simpleName}'."
    )
  }

  override fun parseLiteral(input: Any?): Instant {
    if (input is StringValue) {
        return parseLocalDateTime(input.value)
            ?: throw CoercingParseLiteralException("Unable to turn AST input into a 'Instant' : '$input'")
    }
    throw CoercingParseLiteralException(
        "Expected AST type 'StringValue' but was '${input?.javaClass?.simpleName}'."
    )
  }

  override fun serialize(dataFetcherResult: Any?): Instant {
    return parseLocalDateTime(dataFetcherResult) ?: throw CoercingSerializeException(
        "Expected type 'Instant' but was '${dataFetcherResult?.javaClass?.simpleName}'."
    )
  }
}
