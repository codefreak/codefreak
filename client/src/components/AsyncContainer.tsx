import { Result, Spin } from 'antd'
import { ApolloError } from 'apollo-client'
import React from 'react'

interface AsyncContainerProps {
  data: {
    loading: boolean
    error?: ApolloError
  }
}

const AsyncContainer: React.FC<AsyncContainerProps> = props => {
  if (props.data.loading) {
    return (
      <div style={{ textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }
  if (props.data.error) {
    return (
      <Result
        status="500"
        title="Sorry, something went wrong 😥"
        subTitle={props.data.error.message}
      />
    )
  }
  return <>{props.children}</>
}

export default AsyncContainer
