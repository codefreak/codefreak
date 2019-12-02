import { Result, Spin } from 'antd'
import { ApolloError } from 'apollo-client'
import React from 'react'

interface AsyncPlaceholderProps {
  result: {
    loading: boolean
    error?: ApolloError
  }
}

const AsyncPlaceholder: React.FC<AsyncPlaceholderProps> = props => {
  if (props.result.loading) {
    return (
      <div style={{ textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    )
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
