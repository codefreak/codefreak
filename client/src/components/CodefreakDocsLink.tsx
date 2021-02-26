import React from 'react'

export interface CodefreakDocsLinkProps {
  category?: 'for-admins' | 'for-teachers' | 'for-students' | 'for-developers'
  page?: string
  section?: string
}

const CodefreakDocsLink: React.FC<CodefreakDocsLinkProps> = props => {
  const { category, page = 'index', section, children } = props

  // strip trailing slashes from the base URL
  let link = process.env.CODEFREAK_DOCS_BASE_URL.replace(/^\/+/, '')
  if (category) link += `/${category}`
  link += `/${page}.html`
  if (section) link += `#${section}`

  return (
    <a href={link} target="_blank" rel="noreferrer">
      {children}
    </a>
  )
}

export default CodefreakDocsLink
