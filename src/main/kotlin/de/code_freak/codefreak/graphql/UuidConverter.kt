package de.code_freak.codefreak.graphql

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import java.util.UUID

class UuidConverter : Coercing<UUID, String> {

  private fun parse(input: Any?): UUID? {
    if (input is UUID) return input
    if (input !is String || input.isEmpty()) return null
    return try {
      UUID.fromString(input)
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  override fun parseValue(input: Any?): UUID {
    return parse(input) ?: throw CoercingParseValueException(
        "Expected type 'UUID' but was '${input?.javaClass?.simpleName}'."
    )
  }

  override fun parseLiteral(input: Any?): UUID {
    if (input is StringValue) {
      return parse(input.value)
      ?: throw CoercingParseLiteralException("Unable to turn AST input into a 'UUID' : '$input'")
    }
    throw CoercingParseLiteralException(
        "Expected AST type 'StringValue' but was '${input?.javaClass?.simpleName}'."
    )
  }

  override fun serialize(dataFetcherResult: Any?): String {
    return dataFetcherResult.toString()
  }
}
