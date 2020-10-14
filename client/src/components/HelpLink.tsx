import React from 'react'
import { Link } from 'react-router-dom'

export interface HelpLinkProps {
  category: 'definitions' | 'basics' | 'ide'
  section?: string
}

const HelpLink: React.FC<HelpLinkProps> = props => {
  const { category, section, children } = props
  return (
    <Link
      to={`/help/${category}${section ? '#' + section : ''}`}
      target="_blank"
    >
      {children}
    </Link>
  )
}

export default HelpLink
