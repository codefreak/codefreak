import { Icon } from 'antd'
import { IconProps } from 'antd/lib/icon'
import React from 'react'

export const EvaluationErrorIcon: React.FC<IconProps> = props => {
  const { style, ...restProps } = props
  return (
    <Icon
      type="exclamation-circle"
      style={{ color: 'red', ...style }}
      {...restProps}
    />
  )
}
