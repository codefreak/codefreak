import { Spin } from 'antd'
import React from 'react'
import Centered from './Centered'

type LoadingIndicatorProps = {
  message?: string
}

const LoadingIndicator: React.FC<LoadingIndicatorProps> = ({ message }) => {
  return (
    <Centered>
      <Spin size="large" tip={message} />
    </Centered>
  )
}

export default LoadingIndicator
