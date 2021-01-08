import { ExclamationCircleOutlined } from '@ant-design/icons'
import { IconProps } from '@ant-design/compatible/lib/icon'
import React from 'react'

export const EvaluationErrorIcon: React.FC<IconProps> = props => {
  const { style, ...restProps } = props
  return (
    // <Icon
    //   type="exclamation-circle"
    //   style={{ color: 'red', ...style }}
    //   {...restProps}
    // />
    <ExclamationCircleOutlined
      style={{ color: 'red', ...style }}
      {...restProps}
    />
  )
}
