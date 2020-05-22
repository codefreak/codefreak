import { Layout } from 'antd'
import React from 'react'
import './AppFooter.less'

const repoUrl = 'https://github.com/codefreak/codefreak'

const AppFooter: React.FC = () => {
  const buildYear = process.env.BUILD_YEAR
  // version and hash are exposed by git-revision-webpack-plugin
  const buildVersion = process.env.BUILD_VERSION || 'dev'
  const buildHash = process.env.BUILD_HASH
  const buildVersionLink = buildHash
    ? `${repoUrl}/commit/${buildHash}`
    : repoUrl

  return (
    <Layout.Footer>
      powered by Code FREAK{' '}
      <a href={buildVersionLink} target="_blank" rel="noopener noreferrer">
        v{buildVersion}
      </a>
      <br />© 2019-{buildYear}{' '}
      <a href="https://codefreak.org" target="_blank" rel="noopener noreferrer">
        codefreak.org
      </a>
    </Layout.Footer>
  )
}

export default AppFooter
