import { ApolloProvider } from '@apollo/react-hooks'
import ApolloClient from 'apollo-boost'
import React from 'react'
import ReactDOM from 'react-dom'
import App from './App'
import './index.css'
import { messageService } from './services/message'
import * as serviceWorker from './serviceWorker'

const apolloClient = new ApolloClient({
  uri: '/graphql',
  onError: error => {
    if (!error.operation.getContext().disableGlobalErrorHandling) {
      ;(error.graphQLErrors || []).forEach(err =>
        messageService.error(err.message)
      )
    }
  }
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
