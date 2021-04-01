import { GraphQLError } from 'graphql'
import { ApolloClient, ApolloLink, InMemoryCache, split } from '@apollo/client'
import { onError } from '@apollo/client/link/error'
import { messageService } from './message'
import { getMainDefinition } from '@apollo/client/utilities'
import { createUploadLink } from 'apollo-upload-client'
import { WebSocketLink } from '@apollo/client/link/ws'

export * from '../generated/graphql'

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

export const createApolloClient = (wsLink: WebSocketLink) => {
  return new ApolloClient({
    link: ApolloLink.from([
      onError(error => {
        if (!error.operation.getContext().disableGlobalErrorHandling) {
          // If not authenticated or session expired, reload page to show login dialog
          if ((error as any).networkError?.extensions?.errorCode === '401') {
            window.location.reload()
          }
          messageService.error(extractErrorMessage(error))
        }
      }),
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
