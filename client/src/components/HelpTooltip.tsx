import { QuestionCircleOutlined } from '@ant-design/icons'
import { Tooltip } from 'antd'
import React, { PropsWithChildren } from 'react'
import { AbstractTooltipProps } from 'antd/es/tooltip'

interface HelpBubbleProps extends AbstractTooltipProps {
  title: React.ReactNode
}

const HelpTooltip: React.FC<PropsWithChildren<HelpBubbleProps>> = props => {
  const { title, children, ...tooltipProps } = props
  return (
    <span style={{ cursor: 'help' }}>
      <Tooltip title={title} {...tooltipProps}>
        {children} <QuestionCircleOutlined />
      </Tooltip>
    </span>
  )
}

export default HelpTooltip
