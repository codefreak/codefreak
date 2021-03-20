import React from 'react'
import { trimTrailingSlashes } from '../services/strings'

export interface CodefreakDocsLinkProps {
  category?: 'for-admins' | 'for-teachers' | 'for-students' | 'for-developers'
  page?: string
  section?: string
}

const CodefreakDocsLink: React.FC<CodefreakDocsLinkProps> = props => {
  const { category, page = 'index', section, children } = props

  let link = trimTrailingSlashes(process.env.CODEFREAK_DOCS_BASE_URL)
  if (category) link += `/${category}`
  link += `/${page}.html`
  if (section) link += `#${section}`

  return (
    <a href={link} target="_blank" rel="noreferrer noopener">
      {children}
    </a>
  )
}

export default CodefreakDocsLink
