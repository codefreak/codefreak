import React from 'react'
import { Link } from 'react-router-dom'

export interface SidebarMenuItemProps {
  isUrl?: boolean
  target?: '_blank'
  path: string
}

/**
 * Custom menu item that renders internal links as react-router links
 * and external links with proper target and rel attributes
 */
export const SidebarMenuItem: React.FC<SidebarMenuItemProps> = props => {
  const { isUrl, path, target, children } = props

  if (isUrl) {
    return (
      <a
        href={path}
        target={target}
        rel={target === '_blank' ? 'noreferrer noopener' : undefined}
      >
        {children}
      </a>
    )
  }

  return <Link to={path}>{children}</Link>
}
