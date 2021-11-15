import {
  FileContextType,
  Maybe,
  useStartWorkspaceMutation
} from '../../services/codefreak-api'
import './WorkspacePage.less'
import WorkspaceTabsWrapper from './WorkspaceTabsWrapper'
import { useEffect, useState } from 'react'
import { Col, Row } from 'antd'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import {
  indexOf,
  removeEditorTab,
  WorkspaceTab,
  WorkspaceTabType
} from '../../services/workspace-tabs'
import { useMutableQueryParam } from '../../hooks/useQuery'
import { InstructionsWorkspaceTab } from './tab-panel/InstructionsTabPanel'
import { ShellWorkspaceTab } from './tab-panel/ShellTabPanel'
import { EvaluationWorkspaceTab } from './tab-panel/EvaluationTabPanel'
import { ConsoleWorkspaceTab } from './tab-panel/ConsoleTabPanel'
import { FileTreeWorkspaceTab } from './file-tree/FileTree'
import { EditorWorkspaceTab } from './tab-panel/EditorTabPanel'
import Centered from '../Centered'
import { trimTrailingSlashes, withLeadingSlash } from '../../services/strings'

/**
 * The query parameter fo the active left tab
 */
export const LEFT_TAB_QUERY_PARAM = 'leftTab'

/**
 * The query parameter fo the active right tab
 */
export const RIGHT_TAB_QUERY_PARAM = 'rightTab'

/**
 * Indicates that no tab is active
 */
const NO_ACTIVE_TAB = ''

/**
 * Provides the type of workspace, a callback for when the base-url changes
 * and a button create an answer for the current task
 */
export interface WorkspacePageProps {
  /**
   * The type of workspace
   */
  type: FileContextType

  /**
   * A callback for when the base-url changes
   */
  onBaseUrlChange: (newBaseUrl: string, newAuthToken?: Maybe<string>) => void

  /**
   * A button create an answer for the current task
   */
  createAnswerButton: React.ReactNode

  /**
   * Optional list of files to open initially in editor tabs
   */
  initialOpenFiles?: string[]
}

/**
 * Renders a workspace ide for the current task/answer
 */
const WorkspacePage = ({
  type,
  onBaseUrlChange,
  createAnswerButton,
  initialOpenFiles = []
}: WorkspacePageProps) => {
  const normalizedInitialOpenFiles = initialOpenFiles.map(file =>
    withLeadingSlash(trimTrailingSlashes(file))
  )

  const [activeLeftTab, setActiveLeftTab] = useMutableQueryParam(
    LEFT_TAB_QUERY_PARAM,
    normalizedInitialOpenFiles.length > 0
      ? normalizedInitialOpenFiles[0]
      : NO_ACTIVE_TAB
  )
  const [activeRightTab, setActiveRightTab] = useMutableQueryParam(
    RIGHT_TAB_QUERY_PARAM,
    NO_ACTIVE_TAB
  )

  const initialOpenEditorTabs = normalizedInitialOpenFiles.map(
    file => new EditorWorkspaceTab(file)
  )

  const [leftTabs, setLeftTabs] = useState<WorkspaceTab[]>(
    initialOpenEditorTabs
  )

  const { answerId } = useWorkspace()

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
        .then(result => {
          if (result.data) {
            onBaseUrlChange(
              result.data.startWorkspace.baseUrl,
              result.data?.startWorkspace.authToken
            )
          }
        })
        .catch(() => {
          // Error is caught globally
        })
    }
  })

  useEffect(() => {
    if (activeRightTab === NO_ACTIVE_TAB) {
      setActiveRightTab(WorkspaceTabType.INSTRUCTIONS)
    }
  }, [activeRightTab, setActiveRightTab])

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
        {answerId.length > 0 ? (
          <WorkspaceTabsWrapper tabs={[fileTree]} />
        ) : (
          <Centered>{createAnswerButton}</Centered>
        )}
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
