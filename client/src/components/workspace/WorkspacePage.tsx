import {
  FileContextType,
  useStartWorkspaceMutation
} from '../../services/codefreak-api'
import './WorkspacePage.less'
import WorkspaceTabsWrapper, { WorkspaceTabType } from './WorkspaceTabsWrapper'
import { useEffect, useState } from 'react'
import { Col, Row } from 'antd'
import { WorkspaceContext } from '../../hooks/workspace/useWorkspace'
import { Client, createClient } from 'graphql-ws'
import { graphqlWebSocketPath, normalizePath } from '../../services/workspace'
import { useMutableQueryParam } from '../../hooks/useQuery'
import FileTree from './FileTree'

export interface WorkspacePageProps {
  taskId: string
  answerId: string
  type: FileContextType
}

const WorkspacePage = ({ taskId, answerId, type }: WorkspacePageProps) => {
  const [leftTab, setLeftTab] = useMutableQueryParam('leftTab', 'main.py')
  const [rightTab, setRightTab] = useMutableQueryParam(
    'rightTab',
    WorkspaceTabType.INSTRUCTIONS.toString()
  )

  const [startWorkspace, { data, called }] = useStartWorkspaceMutation({
    variables: {
      context: {
        id: answerId,
        type
      }
    }
  })
  const [baseUrl, setBaseUrl] = useState('')
  const [graphqlWebSocketClient, setGraphqlWebSocketClient] = useState<Client>()

  useEffect(() => {
    if (!data && !called) {
      startWorkspace()
    }
  })

  useEffect(() => {
    if (data && baseUrl.length === 0) {
      setBaseUrl(normalizePath(data.startWorkspace.baseUrl))
    }
  }, [data, baseUrl])

  useEffect(() => {
    if (baseUrl && !graphqlWebSocketClient) {
      const url = graphqlWebSocketPath(baseUrl)
      setGraphqlWebSocketClient(createClient({ url }))
    }
  }, [baseUrl, graphqlWebSocketClient])

  const handleLeftTabChange = (activeKey: string) => setLeftTab(activeKey)
  const handleRightTabChange = (activeKey: string) => setRightTab(activeKey)

  return (
    <WorkspaceContext.Provider
      value={{ baseUrl, graphqlWebSocketClient, taskId, answerId }}
    >
      <Row gutter={4} className="workspace-page">
        <Col span={4}>
          <FileTree />
        </Col>
        <Col span={10}>
          <WorkspaceTabsWrapper
            tabs={[{ type: WorkspaceTabType.EDITOR, path: 'main.py' }]}
            activeTab={leftTab}
            onTabChange={handleLeftTabChange}
          />
        </Col>
        <Col span={10}>
          <WorkspaceTabsWrapper
            tabs={[
              { type: WorkspaceTabType.INSTRUCTIONS },
              { type: WorkspaceTabType.SHELL },
              { type: WorkspaceTabType.EVALUATION }
            ]}
            activeTab={rightTab}
            onTabChange={handleRightTabChange}
          />
        </Col>
      </Row>
    </WorkspaceContext.Provider>
  )
}

export default WorkspacePage
