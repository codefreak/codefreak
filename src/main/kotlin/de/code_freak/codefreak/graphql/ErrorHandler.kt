package de.code_freak.codefreak.graphql

import graphql.ErrorClassification
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.language.SourceLocation
import graphql.servlet.core.DefaultGraphQLErrorHandler
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Component
class ErrorHandler : DefaultGraphQLErrorHandler() {

  private class AccessDeniedError(val ex: ExceptionWhileDataFetching) : GraphQLError {
    override fun getMessage(): String = "Access Denied"
    override fun getErrorType(): ErrorClassification = ex.errorType
    override fun getLocations(): MutableList<SourceLocation> = ex.locations
    override fun getExtensions(): MutableMap<String, Any> = mutableMapOf("errorCode" to "403")
  }

  override fun processErrors(errors: MutableList<GraphQLError>?): MutableList<GraphQLError> {
    val newErrors = errors?.map {
      if (it is ExceptionWhileDataFetching && it.exception is AccessDeniedException) {
        AccessDeniedError(it)
      } else it
    }
    return super.processErrors(newErrors)
  }
}
