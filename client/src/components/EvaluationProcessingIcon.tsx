import { SettingOutlined } from '@ant-design/icons'
import React, { ComponentProps } from 'react'

const EvaluationProcessingIcon: React.FC<
  ComponentProps<typeof SettingOutlined>
> = ({ className, ...props }) => {
  const classes = 'spin-slow ' + (className ? className : '')
  return <SettingOutlined spin className={classes} {...props} />
}

export default EvaluationProcessingIcon
