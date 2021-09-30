import {
  FileContextType,
  useStartWorkspaceMutation
} from '../../services/codefreak-api'
import './WorkspacePage.less'
import WorkspaceTabsWrapper from './WorkspaceTabsWrapper'
import { useEffect, useState } from 'react'
import { Col, Row } from 'antd'
import { WorkspaceContext } from '../../hooks/workspace/useWorkspace'
import { Client, createClient } from 'graphql-ws'
import {
  getTabIndex,
  graphqlWebSocketPath,
  LEFT_TAB_QUERY_PARAM,
  normalizePath,
  removeEditorTab,
  RIGHT_TAB_QUERY_PARAM,
  toActiveTabQueryParam,
  WorkspaceTab,
  WorkspaceTabFactory,
  WorkspaceTabType
} from '../../services/workspace'
import { useMutableQueryParam } from '../../hooks/useQuery'
import FileTree from './FileTree'

export interface WorkspacePageProps {
  taskId: string
  answerId: string
  type: FileContextType
}

const DEFAULT_RIGHT_TABS = [
  WorkspaceTabFactory.InstructionsTab(),
  WorkspaceTabFactory.ShellTab(),
  WorkspaceTabFactory.EvaluationTab()
]

const WorkspacePage = ({ taskId, answerId, type }: WorkspacePageProps) => {
  const [activeLeftTab, setActiveLeftTab] = useMutableQueryParam(
    LEFT_TAB_QUERY_PARAM,
    ''
  )
  const [activeRightTab, setActiveRightTab] = useMutableQueryParam(
    RIGHT_TAB_QUERY_PARAM,
    ''
  )

  const [leftTabs, setLeftTabs] = useState<WorkspaceTab[]>([])
  // These are not changeable for now
  const rightTabs = DEFAULT_RIGHT_TABS

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

    if (activeRightTab === '') {
      setActiveRightTab(WorkspaceTabType.INSTRUCTIONS)
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

  useEffect(() => {
    const isNotEmpty =
      activeLeftTab !== WorkspaceTabType.EMPTY && activeLeftTab !== ''
    const isNotOpen =
      getTabIndex(leftTabs, WorkspaceTabFactory.EditorTab(activeLeftTab)) === -1
    if (isNotEmpty && isNotOpen) {
      setLeftTabs(prevState => [
        ...prevState,
        WorkspaceTabFactory.EditorTab(activeLeftTab)
      ])
    }
  }, [leftTabs, activeLeftTab, setActiveLeftTab])

  const handleLeftTabChange = (activeKey: string) => setActiveLeftTab(activeKey)
  const handleRightTabChange = (activeKey: string) =>
    setActiveRightTab(activeKey)

  const handleOpenFile = (path: string) => {
    setLeftTabs(prevState => {
      const isTabOpen =
        getTabIndex(prevState, WorkspaceTabFactory.EditorTab(path)) !== -1

      if (!isTabOpen) {
        return [...prevState, WorkspaceTabFactory.EditorTab(path)]
      }

      return prevState
    })

    setActiveLeftTab(path)
  }

  const handleCloseLeftTab = (path: string) => {
    setLeftTabs(prevState => {
      const newState = removeEditorTab(path, prevState)

      const closedTabIndex = getTabIndex(
        prevState,
        WorkspaceTabFactory.EditorTab(path)
      )

      const hasClosedTab = closedTabIndex !== -1
      const hasNewTabs = newState.length > 0
      const shouldChangeActiveTab = activeLeftTab === path

      if (hasClosedTab && hasNewTabs && shouldChangeActiveTab) {
        const newActiveLeftTabIndex =
          closedTabIndex !== 0 ? closedTabIndex - 1 : 0
        const newActiveLeftWorkspaceTab = newState[newActiveLeftTabIndex]

        setActiveLeftTab(toActiveTabQueryParam(newActiveLeftWorkspaceTab))
      }

      if (newState.length === 0) {
        setActiveLeftTab(WorkspaceTabType.EMPTY)
      }

      return newState
    })
  }

  return (
    <WorkspaceContext.Provider
      value={{ baseUrl, graphqlWebSocketClient, taskId, answerId }}
    >
      <Row gutter={4} className="workspace-page">
        <Col span={4}>
          <FileTree onOpenFile={handleOpenFile} />
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
    </WorkspaceContext.Provider>
  )
}

export default WorkspacePage
