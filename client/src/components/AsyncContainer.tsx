import { ApolloError } from '@apollo/client'
import { Result } from 'antd'
import React from 'react'
import LoadingIndicator from './LoadingIndicator'

interface AsyncPlaceholderProps {
  result: {
    loading: boolean
    error?: ApolloError
  }
}

const AsyncPlaceholder: React.FC<AsyncPlaceholderProps> = props => {
  if (props.result.loading) {
    return <LoadingIndicator />
  }
  if (props.result.error) {
    return (
      <Result
        status="500"
        title="Sorry, something went wrong 😥"
        subTitle={props.result.error.message}
      />
    )
  }
  return <>{props.children}</>
}

export default AsyncPlaceholder
