import { Tabs, Tooltip } from 'antd'
import {
  extractRelativeFilePath,
  readFilePath,
  WorkspaceTab,
  WorkspaceTabFactory,
  WorkspaceTabType
} from '../../services/workspace'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import EditorTabPanel from './EditorTabPanel'
import EmptyTabPanel from './EmptyTabPanel'
import InstructionsTabPanel from './InstructionsTabPanel'
import ShellTabPanel from './ShellTabPanel'
import EvaluationTabPanel from './EvaluationTabPanel'
import { noop } from '../../services/util'
import EvaluationIndicator from '../EvaluationIndicator'
import {
  CodeOutlined,
  DashboardOutlined,
  FileTextOutlined,
  SolutionOutlined
} from '@ant-design/icons'

const getTabTitle = (
  type: WorkspaceTabType,
  filePath = '',
  loading = false,
  answerId = ''
) => {
  switch (type) {
    case WorkspaceTabType.EDITOR:
      if (loading) {
        return (
          <>
            <FileTextOutlined /> Loading...
          </>
        )
      }
      if (filePath.length > 0) {
        return (
          <>
            <FileTextOutlined /> {extractRelativeFilePath(filePath)}
          </>
        )
      }

      throw new Error('tab is set to type EDITOR but no filePath was given')
    case WorkspaceTabType.INSTRUCTIONS:
      return (
        <>
          <SolutionOutlined /> Instructions
        </>
      )
    case WorkspaceTabType.SHELL:
      return (
        <>
          <CodeOutlined /> Shell
        </>
      )
    case WorkspaceTabType.EVALUATION:
      return (
        <>
          <DashboardOutlined /> Evaluation-Results{' '}
          <EvaluationIndicator style={{ marginLeft: 8 }} answerId={answerId} />
        </>
      )
    case WorkspaceTabType.EMPTY:
    default:
      return 'No files open'
  }
}

const renderTab =
  (baseUrl: string, loading: boolean, answerId: string) =>
  ({ path, type }: WorkspaceTab) => {
    const title = getTabTitle(
      type,
      readFilePath(baseUrl, path ?? ''),
      loading,
      answerId
    )

    let content

    switch (type) {
      case WorkspaceTabType.EDITOR:
        if (path === undefined) {
          throw new Error('Editor tab must have a path specified!')
        }

        content = <EditorTabPanel file={path} />
        break
      case WorkspaceTabType.INSTRUCTIONS:
        content = <InstructionsTabPanel />
        break
      case WorkspaceTabType.SHELL:
        content = <ShellTabPanel />
        break
      case WorkspaceTabType.EVALUATION:
        content = <EvaluationTabPanel />
        break
      case WorkspaceTabType.EMPTY:
      default:
        content = <EmptyTabPanel loading={loading} />
        break
    }

    const key = path ?? type
    const isEditorTab = type === WorkspaceTabType.EDITOR

    return (
      <Tabs.TabPane
        tab={
          <Tooltip title={title} placement="top" visible={false}>
            <span>{title}</span>
          </Tooltip>
        }
        key={key}
        style={{ height: '100%' }}
        disabled={loading}
        closable={isEditorTab}
      >
        {content}
      </Tabs.TabPane>
    )
  }

interface WorkspaceTabsWrapperProps {
  tabs: WorkspaceTab[]
  activeTab?: string | WorkspaceTabType
  onTabChange?: (activeKey: string) => void
  onTabClose?: (key: string) => void
}

const WorkspaceTabsWrapper = ({
  tabs,
  activeTab,
  onTabChange = noop,
  onTabClose = noop
}: WorkspaceTabsWrapperProps) => {
  const { isAvailable, baseUrl, answerId } = useWorkspace()

  const renderTabImpl = renderTab(baseUrl, !isAvailable, answerId)

  const renderedTabs =
    tabs.length > 0
      ? tabs.map(renderTabImpl)
      : renderTabImpl(WorkspaceTabFactory.EmptyTab())

  const handleEdit = (
    e: React.MouseEvent | React.KeyboardEvent | string,
    action: 'add' | 'remove'
  ) => {
    if (action === 'remove' && typeof e === 'string') {
      onTabClose(e)
    }
  }

  return (
    <div className="workspace-tabs-wrapper">
      <Tabs
        hideAdd
        type="editable-card"
        className="workspace-tabs"
        activeKey={activeTab && activeTab.length > 0 ? activeTab : undefined}
        onChange={onTabChange}
        onEdit={handleEdit}
      >
        {renderedTabs}
      </Tabs>
    </div>
  )
}

export default WorkspaceTabsWrapper
