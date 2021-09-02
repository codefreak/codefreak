import {
  FileContextType,
  useStartWorkspaceMutation
} from '../../generated/graphql'
import './WorkspacePage.less'
import WorkspaceTabsWrapper, { WorkspaceTabType } from './WorkspaceTabsWrapper'
import { useEffect, useState } from 'react'
import { Col, Row } from 'antd'

export interface WorkspacePageProps {
  id: string
  type: FileContextType
}

const WorkspacePage = ({ id, type }: WorkspacePageProps) => {
  const [startWorkspace, { data, called }] = useStartWorkspaceMutation({
    variables: {
      context: {
        id,
        type
      }
    }
  })
  const [baseUrl, setBaseUrl] = useState('')

  useEffect(() => {
    if (!data && !called) {
      startWorkspace()
    }
  })

  useEffect(() => {
    if (data && baseUrl.length === 0) {
      setBaseUrl(data.startWorkspace.baseUrl)
    }
  }, [data, baseUrl])

  return (
    <Row gutter={16} className="workspace-page">
      <Col span={12}>
        <WorkspaceTabsWrapper
          baseUrl={baseUrl}
          tabs={[
            { type: WorkspaceTabType.EDITOR, path: 'main.py' },
            { type: WorkspaceTabType.EDITOR, path: 'main.java' },
            { type: WorkspaceTabType.EDITOR, path: 'test.js' },
            { type: WorkspaceTabType.EDITOR, path: 'README.md' }
          ]}
        />
      </Col>
      <Col span={12}>
        <WorkspaceTabsWrapper
          baseUrl={baseUrl}
          tabs={[{ type: WorkspaceTabType.INSTRUCTIONS }]}
        />
      </Col>
    </Row>
  )
}

export default WorkspacePage
