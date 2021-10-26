import {
  FileContextType,
  useStartWorkspaceMutation
} from '../../services/codefreak-api'
import './WorkspacePage.less'
import WorkspaceTabsWrapper from './WorkspaceTabsWrapper'
import { useEffect, useState } from 'react'
import { Col, Row } from 'antd'
import useWorkspace, {
  NO_AUTH_TOKEN,
  NO_BASE_URL
} from '../../hooks/workspace/useWorkspace'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import { EditorWorkspaceTab } from './EditorTabPanel'
import { useMutableQueryParam } from '../../hooks/useQuery'
import { InstructionsWorkspaceTab } from './InstructionsTabPanel'
import { ShellWorkspaceTab } from './ShellTabPanel'
import { EvaluationWorkspaceTab } from './EvaluationTabPanel'
import { ConsoleWorkspaceTab } from './ConsoleTabPanel'
import Centered from '../Centered'

export const RIGHT_TAB_QUERY_PARAM = 'rightTab'

const NO_ACTIVE_TAB = ''

export interface WorkspacePageProps {
  type: FileContextType
  onBaseUrlChange: (newBaseUrl: string, newAuthToken: string) => void
  createAnswerButton: React.ReactNode
}

const WorkspacePage = ({
  type,
  onBaseUrlChange,
  createAnswerButton
}: WorkspacePageProps) => {
  const [activeRightTab, setActiveRightTab] = useMutableQueryParam(
    RIGHT_TAB_QUERY_PARAM,
    ''
  )

  const [leftTabs] = useState<WorkspaceTab[]>([
    new EditorWorkspaceTab('main.py')
  ])

  const { baseUrl, answerId } = useWorkspace()

  // These are not changeable for now
  const rightTabs = [
    new InstructionsWorkspaceTab(),
    new ShellWorkspaceTab(),
    new ConsoleWorkspaceTab(),
    new EvaluationWorkspaceTab(answerId)
  ]

  const [startWorkspace, { data, called }] = useStartWorkspaceMutation()

  useEffect(() => {
    if (!data && !called && answerId.length > 0) {
      startWorkspace({
        variables: {
          context: {
            id: answerId,
            type
          }
        }
      })
    }
  })

  useEffect(() => {
    if (activeRightTab === NO_ACTIVE_TAB) {
      setActiveRightTab(WorkspaceTabType.INSTRUCTIONS)
    }
  }, [activeRightTab, setActiveRightTab])

  useEffect(() => {
    if (data && baseUrl === NO_BASE_URL) {
      onBaseUrlChange(
        data.startWorkspace.baseUrl,
        data.startWorkspace.authToken ?? NO_AUTH_TOKEN
      )
    }
  }, [data, baseUrl, onBaseUrlChange])

  const handleRightTabChange = (activeKey: string) =>
    setActiveRightTab(activeKey)

  return (
    <Row gutter={4} className="workspace-page">
      <Col span={12}>
        {answerId.length > 0 ? (
          <WorkspaceTabsWrapper tabs={leftTabs} />
        ) : (
          <Centered>{createAnswerButton}</Centered>
        )}
      </Col>
      <Col span={12}>
        <WorkspaceTabsWrapper
          tabs={rightTabs}
          activeTab={activeRightTab}
          onTabChange={handleRightTabChange}
        />
      </Col>
    </Row>
  )
}

export default WorkspacePage
