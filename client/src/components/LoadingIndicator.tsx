import { Spin } from 'antd'
import React from 'react'
import Centered from './Centered'

const LoadingIndicator: React.FC = () => {
  return (
    <Centered>
      <Spin size="large" />
    </Centered>
  )
}

export default LoadingIndicator
