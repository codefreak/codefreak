import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import { Link } from 'react-router-dom'

interface HelpButtonProps extends ButtonProps {
  category: 'definitions' | 'basics'
  section?: string
}

const HelpButton: React.FC<HelpButtonProps> = props => {
  const { category, section, children, ...buttonProps } = props
  return (
    <Link
      to={`/help/${category}${section ? '#' + section : ''}`}
      target="_blank"
    >
      <Button icon="question-circle" {...buttonProps}>
        {children}
      </Button>
    </Link>
  )
}

export default HelpButton
