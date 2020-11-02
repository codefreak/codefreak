package org.codefreak.codefreak.graphql

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import java.util.UUID
import org.codefreak.codefreak.util.UuidUtil

class UuidConverter : Coercing<UUID, String> {

  override fun parseValue(input: Any?): UUID {
    return UuidUtil.parse(input) ?: throw CoercingParseValueException(
        "Expected type 'UUID' but was '${input?.javaClass?.simpleName}'."
    )
  }

  override fun parseLiteral(input: Any?): UUID {
    if (input is StringValue) {
      return UuidUtil.parse(input.value)
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
