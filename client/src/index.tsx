import {
  ApolloClient,
  ApolloLink,
  ApolloProvider,
  InMemoryCache,
  split
} from '@apollo/client'
import { onError } from '@apollo/client/link/error'
import { WebSocketLink } from '@apollo/client/link/ws'
import { getMainDefinition } from '@apollo/client/utilities'
import { createUploadLink } from 'apollo-upload-client'
import React from 'react'
import ReactDOM from 'react-dom'
import App from './App'
import './index.css'
import { extractErrorMessage } from './services/codefreak-api'
import { messageService } from './services/message'
import * as serviceWorker from './serviceWorker'

const httpLink = createUploadLink({ uri: '/graphql', credentials: 'include' })

const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'

const wsLink = new WebSocketLink({
  uri: `${wsProtocol}//${window.location.host}/subscriptions`,
  options: {
    reconnect: true,
    connectionParams: {
      credentials: 'include'
    }
  }
})

// Based on https://github.com/apollographql/apollo-link/issues/197#issuecomment-363387875
// We need to re-connect the websocket so that the new session cookie is used for the handshake
const resetWebsocket = () => {
  const websocket = (wsLink as any).subscriptionClient
  websocket.close(true, true)
  websocket.connect()
}

const apolloClient = new ApolloClient({
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
      httpLink
    )
  ]),
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network'
    }
  }
})

const onUserChanged = () => {
  resetWebsocket()
  apolloClient.clearStore()
}

ReactDOM.render(
  <ApolloProvider client={apolloClient}>
    <App onUserChanged={onUserChanged} />
  </ApolloProvider>,
  document.getElementById('root')
)

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister()
