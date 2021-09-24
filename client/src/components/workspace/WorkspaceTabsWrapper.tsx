import { Tabs } from 'antd'
import { extractRelativeFilePath, readFilePath } from '../../services/workspace'
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

export enum WorkspaceTabType {
  EDITOR = 'editor',
  EMPTY = 'empty',
  INSTRUCTIONS = 'instructions',
  SHELL = 'shell',
  EVALUATION = 'evaluation'
}

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

type WorkspaceTab = {
  type: WorkspaceTabType
  path?: string
}

const renderTab =
  (baseUrl: string, loading: boolean, answerId: string) =>
  (tab: WorkspaceTab) => {
    const title = getTabTitle(
      tab.type,
      readFilePath(baseUrl, tab.path ?? ''),
      loading,
      answerId
    )

    let content

    switch (tab.type) {
      case WorkspaceTabType.EDITOR:
        content = <EditorTabPanel file={tab.path ?? ''} />
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

    const key = tab.path ?? tab.type

    return (
      <Tabs.TabPane
        tab={title}
        key={key}
        style={{ height: '100%' }}
        disabled={loading}
        closable={tab.type === WorkspaceTabType.EDITOR}
      >
        {content}
      </Tabs.TabPane>
    )
  }

interface WorkspaceTabsWrapperProps {
  tabs: WorkspaceTab[]
  activeTab?: string | WorkspaceTabType
  onTabChange?: (activeKey: string) => void
}

const WorkspaceTabsWrapper = ({
  tabs,
  activeTab,
  onTabChange = noop
}: WorkspaceTabsWrapperProps) => {
  const { isAvailable, baseUrl, answerId } = useWorkspace()

  const renderTabImpl = renderTab(baseUrl, !isAvailable, answerId)

  const renderedTabs =
    tabs.length > 0
      ? tabs.map(renderTabImpl)
      : renderTabImpl({ type: WorkspaceTabType.EMPTY })

  return (
    <div className="workspace-tabs-wrapper">
      <Tabs
        hideAdd
        type="editable-card"
        className="workspace-tabs"
        activeKey={activeTab}
        onChange={onTabChange}
      >
        {renderedTabs}
      </Tabs>
    </div>
  )
}

export default WorkspaceTabsWrapper
