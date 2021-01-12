import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import HelpLink, { HelpLinkProps } from './HelpLink'
import { QuestionCircleOutlined } from '@ant-design/icons'

export interface HelpButtonProps extends ButtonProps, HelpLinkProps {}

const HelpButton: React.FC<HelpButtonProps> = props => {
  const { category, section, children, ...buttonProps } = props
  return (
    <HelpLink section={section} category={category}>
      <Button icon={<QuestionCircleOutlined />} {...buttonProps}>
        {children}
      </Button>
    </HelpLink>
  )
}

export default HelpButton
