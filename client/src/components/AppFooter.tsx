import { Layout, Tooltip } from 'antd'
import React from 'react'
import './AppFooter.less'
import preval from 'preval.macro'

const repoUrl = 'https://github.com/code-freak/code-freak'

const AppFooter: React.FC = () => {
  const buildYear = preval`module.exports = new Date().getFullYear();`
  const buildVersion = preval`module.exports = (process.env.GIT_TAG ? 'v' + process.env.GIT_TAG : undefined) || "[dev]"`
  const buildHash = process.env.GIT_COMMIT
  const buildVersionLink = buildHash
    ? `${repoUrl}/commit/${buildHash}`
    : repoUrl

  return (
    <Layout.Footer>
      powered by Code FREAK{' '}
      <Tooltip title={buildHash}>
        <a href={buildVersionLink} target="_blank" rel="noopener noreferrer">
          {buildVersion}
        </a>
      </Tooltip>
      <br />© 2019-{buildYear}{' '}
      <a href="https://codefreak.org" target="_blank" rel="noopener noreferrer">
        codefreak.org
      </a>
    </Layout.Footer>
  )
}

export default AppFooter
