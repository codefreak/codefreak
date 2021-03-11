import {
  ApolloClient,
  ApolloLink,
  InMemoryCache,
  ServerError,
  split
} from '@apollo/client'
import { ErrorHandler, ErrorResponse, onError } from '@apollo/client/link/error'
import { messageService } from './message'
import { getMainDefinition } from '@apollo/client/utilities'
import { createUploadLink } from 'apollo-upload-client'
import { WebSocketLink } from '@apollo/client/link/ws'
import { GraphQLError } from 'graphql'

export * from '../generated/graphql'

const isNetworkError = (
  error: any
): error is Pick<ServerError, 'response' | 'statusCode' | 'message'> => {
  return (
    typeof error === 'object' &&
    'response' in error &&
    'statusCode' in error &&
    'message' in error
  )
}

export const extractErrorMessage = (error: {
  graphQLErrors?: ReadonlyArray<GraphQLError>
}) => {
  return (error.graphQLErrors || []).map(e => e.message).join('\n')
}

export const createHttpLink = () =>
  createUploadLink({ uri: '/graphql', credentials: 'include' })

export const createApolloWsLink = () => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return new WebSocketLink({
    uri: `${wsProtocol}//${window.location.host}/subscriptions`,
    options: {
      reconnect: true,
      connectionParams: {
        credentials: 'include'
      }
    }
  })
}

const extractErrorMessageFromResponse = (error: ErrorResponse) => {
  const { graphQLErrors, networkError } = error
  if (graphQLErrors !== undefined) {
    return extractErrorMessage({ graphQLErrors })
  }
  if (isNetworkError(networkError)) {
    const { statusCode, message } = networkError
    return `Error ${statusCode}: ${message}`
  }
  return 'Something went wrong. Please try again.'
}

/**
 * Global Apollo error handler that shows an error notification
 * at the top of the screen.
 * There is a bug (or feature?) in Apollo where this error handling is never
 * applied to mutations:
 * https://github.com/apollographql/apollo-client/issues/6070
 *
 * @param error
 */
const apolloErrorHandler: ErrorHandler = error => {
  if (!error.operation.getContext().disableGlobalErrorHandling) {
    // If not authenticated or session expired, reload page to show login dialog
    const { networkError } = error
    if (isNetworkError(networkError) && networkError.statusCode === 401) {
      window.location.reload()
      return
    }
    messageService.error(extractErrorMessageFromResponse(error))
  }
}

export const createApolloClient = (wsLink: WebSocketLink) => {
  return new ApolloClient({
    link: ApolloLink.from([
      onError(apolloErrorHandler),
      split(
        ({ query }) => {
          const definition = getMainDefinition(query)
          return (
            definition.kind === 'OperationDefinition' &&
            definition.operation === 'subscription'
          )
        },
        wsLink,
        createHttpLink()
      )
    ]),
    cache: new InMemoryCache(),
    defaultOptions: {
      watchQuery: {
        fetchPolicy: 'cache-and-network'
      }
    }
  })
}
