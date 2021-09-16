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
import { messageService } from '../../services/message'

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
  const [graphqlWebSocketClient, setGraphqlWebSocketClient] = useState<Client>()

  useEffect(() => {
    if (!data && !called) {
      startWorkspace().then(() => messageService.success('Workspace started'))
    }
  })

  useEffect(() => {
    if (data && baseUrl.length === 0) {
      setBaseUrl(
        normalizePath(
          data.startWorkspace.baseUrl.replace(
            'minikube.global',
            'minikube.global:8081'
          )
        )
      )
    }
  }, [data, baseUrl])

  useEffect(() => {
    if (baseUrl && !graphqlWebSocketClient) {
      const url = graphqlWebSocketPath(baseUrl)
      setGraphqlWebSocketClient(createClient({ url }))
    }
  }, [baseUrl, graphqlWebSocketClient])

  return (
    <WorkspaceContext.Provider value={{ baseUrl, graphqlWebSocketClient }}>
      <Row gutter={16} className="workspace-page">
        <Col span={12}>
          <WorkspaceTabsWrapper
            tabs={[{ type: WorkspaceTabType.EDITOR, path: 'main.py' }]}
          />
        </Col>
        <Col span={12}>
          <WorkspaceTabsWrapper
            tabs={[
              { type: WorkspaceTabType.INSTRUCTIONS },
              { type: WorkspaceTabType.SHELL }
            ]}
          />
        </Col>
      </Row>
    </WorkspaceContext.Provider>
  )
}

export default WorkspacePage
