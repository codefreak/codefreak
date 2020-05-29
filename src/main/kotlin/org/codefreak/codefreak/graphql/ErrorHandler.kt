package org.codefreak.codefreak.graphql

import org.codefreak.codefreak.service.ResourceLimitException
import graphql.ErrorClassification
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.language.SourceLocation
import graphql.servlet.core.DefaultGraphQLErrorHandler
import org.codefreak.codefreak.auth.NotAuthenticatedException
import org.codefreak.codefreak.service.EntityNotFoundException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component

@Component
class ErrorHandler : DefaultGraphQLErrorHandler() {

  private class CustomError(val ex: ExceptionWhileDataFetching, val customMessage: String, val code: String) : GraphQLError {
    override fun getMessage(): String = customMessage
    override fun getErrorType(): ErrorClassification = ex.errorType
    override fun getLocations(): MutableList<SourceLocation> = ex.locations
    override fun getExtensions(): MutableMap<String, Any> = mutableMapOf("errorCode" to code)
  }

  override fun processErrors(errors: MutableList<GraphQLError>?): MutableList<GraphQLError> {
    val newErrors = errors?.map {
      if (it is ExceptionWhileDataFetching) {
        when (it.exception) {
          is EntityNotFoundException -> CustomError(it, it.message, "404")
          is NotAuthenticatedException -> CustomError(it, it.message, "401")
          is AccessDeniedException -> CustomError(it, "Access Denied", "403")
          is BadCredentialsException -> CustomError(it, "Bad Credentials", "422")
          is ResourceLimitException -> CustomError(it, it.message, "503")
          is IllegalArgumentException, is IllegalStateException -> CustomError(it, it.message, "422")
          else -> it
        }
      } else it
    }
    return super.processErrors(newErrors)
  }
}
