import { ExclamationCircleOutlined } from '@ant-design/icons'
import React, { ComponentProps } from 'react'

export const EvaluationErrorIcon: React.FC<
  ComponentProps<typeof ExclamationCircleOutlined>
> = props => {
  const { style, ...restProps } = props
  return (
    <ExclamationCircleOutlined
      style={{ color: 'red', ...style }}
      {...restProps}
    />
  )
}
