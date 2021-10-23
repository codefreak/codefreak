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
import {
  indexOf,
  removeEditorTab,
  WorkspaceTab,
  WorkspaceTabType
} from '../../services/workspace-tabs'
import { useMutableQueryParam } from '../../hooks/useQuery'
import { InstructionsWorkspaceTab } from './InstructionsTabPanel'
import { ShellWorkspaceTab } from './ShellTabPanel'
import { EvaluationWorkspaceTab } from './EvaluationTabPanel'
import { ConsoleWorkspaceTab } from './ConsoleTabPanel'
import { FileTreeWorkspaceTab } from './FileTree'
import { EditorWorkspaceTab } from './EditorTabPanel'

export const LEFT_TAB_QUERY_PARAM = 'leftTab'
export const RIGHT_TAB_QUERY_PARAM = 'rightTab'

const NO_ACTIVE_TAB = ''

export interface WorkspacePageProps {
  type: FileContextType
  onBaseUrlChange: (newBaseUrl: string, newAuthToken: string) => void
}

const WorkspacePage = ({ type, onBaseUrlChange }: WorkspacePageProps) => {
  const [activeLeftTab, setActiveLeftTab] = useMutableQueryParam(
    LEFT_TAB_QUERY_PARAM,
    NO_ACTIVE_TAB
  )
  const [activeRightTab, setActiveRightTab] = useMutableQueryParam(
    RIGHT_TAB_QUERY_PARAM,
    NO_ACTIVE_TAB
  )

  const [leftTabs, setLeftTabs] = useState<WorkspaceTab[]>([])

  const { baseUrl, answerId } = useWorkspace()

  // These are not changeable for now
  const rightTabs = [
    new InstructionsWorkspaceTab(),
    new ShellWorkspaceTab(),
    new ConsoleWorkspaceTab(),
    new EvaluationWorkspaceTab(answerId)
  ]

  const [startWorkspace, { data, called }] = useStartWorkspaceMutation({
    variables: {
      context: {
        id: answerId,
        type
      }
    }
  })

  useEffect(() => {
    if (!data && !called) {
      startWorkspace()
    }

    if (activeRightTab === NO_ACTIVE_TAB) {
      setActiveRightTab(WorkspaceTabType.INSTRUCTIONS)
    }
  })

  useEffect(() => {
    if (data && baseUrl === NO_BASE_URL) {
      onBaseUrlChange(
        data.startWorkspace.baseUrl,
        data.startWorkspace.authToken ?? NO_AUTH_TOKEN
      )
    }
  }, [data, baseUrl, onBaseUrlChange])

  useEffect(() => {
    const isNotEmpty =
      activeLeftTab !== WorkspaceTabType.EMPTY &&
      activeLeftTab !== NO_ACTIVE_TAB
    const isNotOpen =
      indexOf(leftTabs, new EditorWorkspaceTab(activeLeftTab)) === -1
    if (isNotEmpty && isNotOpen) {
      setLeftTabs(prevState => [
        ...prevState,
        new EditorWorkspaceTab(activeLeftTab)
      ])
    }
  }, [leftTabs, activeLeftTab, setActiveLeftTab])

  const handleLeftTabChange = (activeKey: string) => setActiveLeftTab(activeKey)
  const handleRightTabChange = (activeKey: string) =>
    setActiveRightTab(activeKey)

  const handleOpenFile = (path: string) => {
    setLeftTabs(prevState => {
      const isTabOpen = indexOf(prevState, new EditorWorkspaceTab(path)) !== -1

      if (!isTabOpen) {
        return [...prevState, new EditorWorkspaceTab(path)]
      }

      return prevState
    })

    setActiveLeftTab(path)
  }

  const handleCloseLeftTab = (path: string) => {
    setLeftTabs(prevState => {
      const newState = removeEditorTab(path, prevState)

      const closedTabIndex = indexOf(prevState, new EditorWorkspaceTab(path))

      const hasClosedTab = closedTabIndex !== -1
      const hasNewTabs = newState.length > 0
      const shouldChangeActiveTab = activeLeftTab === path

      if (hasClosedTab && hasNewTabs && shouldChangeActiveTab) {
        const newActiveLeftTabIndex =
          closedTabIndex !== 0 ? closedTabIndex - 1 : 0
        const newActiveLeftWorkspaceTab = newState[newActiveLeftTabIndex]

        setActiveLeftTab(newActiveLeftWorkspaceTab.toActiveTabQueryParam())
      }

      if (newState.length === 0) {
        setActiveLeftTab(WorkspaceTabType.EMPTY)
      }

      return newState
    })
  }

  const fileTree = new FileTreeWorkspaceTab(handleOpenFile)

  return (
    <Row gutter={4} className="workspace-page">
      <Col span={4}>
        <WorkspaceTabsWrapper tabs={[fileTree]} />
      </Col>
      <Col span={10}>
        <WorkspaceTabsWrapper
          tabs={leftTabs}
          activeTab={activeLeftTab}
          onTabChange={handleLeftTabChange}
          onTabClose={handleCloseLeftTab}
        />
      </Col>
      <Col span={10}>
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
