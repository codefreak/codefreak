import { ApolloProvider } from '@apollo/react-hooks'
import { InMemoryCache } from 'apollo-cache-inmemory'
import ApolloClient from 'apollo-client'
import { ApolloLink } from 'apollo-link'
import { onError } from 'apollo-link-error'
import { createUploadLink } from 'apollo-upload-client'
import React from 'react'
import ReactDOM from 'react-dom'
import App from './App'
import './index.css'
import { messageService } from './services/message'
import * as serviceWorker from './serviceWorker'

const apolloClient = new ApolloClient({
  link: ApolloLink.from([
    onError(error => {
      if (!error.operation.getContext().disableGlobalErrorHandling) {
        ;(error.graphQLErrors || []).forEach(err =>
          messageService.error(err.message)
        )
      }
    }),
    createUploadLink({ uri: '/graphql', credentials: 'include' })
  ]),
  cache: new InMemoryCache()
})

ReactDOM.render(
  <ApolloProvider client={apolloClient}>
    <App />
  </ApolloProvider>,
  document.getElementById('root')
)

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister()
