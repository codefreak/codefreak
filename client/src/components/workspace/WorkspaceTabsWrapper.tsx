import { Tabs } from 'antd'
import { extractRelativeFilePath, readFilePath } from '../../services/workspace'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import EditorTabPanel from './EditorTabPanel'
import EmptyTabPanel from './EmptyTabPanel'
import InstructionsTabPanel from './InstructionsTabPanel'

export enum WorkspaceTabType {
  EDITOR,
  EMPTY,
  INSTRUCTIONS
}

const getTabTitle = (
  type: WorkspaceTabType,
  filePath = '',
  loading = false
) => {
  if (loading) {
    return 'Loading...'
  }

  switch (type) {
    case WorkspaceTabType.EMPTY:
      return 'No files open'
    case WorkspaceTabType.EDITOR:
      if (filePath.length > 0) {
        return extractRelativeFilePath(filePath)
      } else {
        throw new Error('tab is set to type EDITOR but no filePath was given')
      }
    case WorkspaceTabType.INSTRUCTIONS:
      return 'Instructions'
    default:
      return ''
  }
}

type WorkspaceTab = {
  type: WorkspaceTabType
  path?: string
}

const renderTab =
  (baseUrl: string, loading: boolean) =>
  (tab: WorkspaceTab, index = 0) => {
    const title = getTabTitle(
      tab.type,
      readFilePath(baseUrl, tab.path ?? ''),
      loading
    )

    let content

    switch (tab.type) {
      case WorkspaceTabType.EDITOR:
        content = <EditorTabPanel file={tab.path ?? ''} />
        break
      case WorkspaceTabType.INSTRUCTIONS:
        content = <InstructionsTabPanel loading={loading} />
        break
      case WorkspaceTabType.EMPTY:
      default:
        content = <EmptyTabPanel loading={loading} />
        break
    }

    return (
      <Tabs.TabPane
        tab={title}
        key={`tab-${title}-${index}`}
        style={{ height: '100%' }}
      >
        {content}
      </Tabs.TabPane>
    )
  }

type WorkspaceTabsWrapperProps = {
  tabs: WorkspaceTab[]
}

const WorkspaceTabsWrapper = ({ tabs }: WorkspaceTabsWrapperProps) => {
  const { isAvailable, baseUrl } = useWorkspace()

  const renderTabImpl = renderTab(baseUrl, !isAvailable)

  const renderedTabs =
    tabs.length > 0
      ? tabs.map(renderTabImpl)
      : renderTabImpl({ type: WorkspaceTabType.EMPTY })

  return (
    <div className="workspace-tabs-wrapper">
      <Tabs hideAdd type="card" className="workspace-tabs">
        {renderedTabs}
      </Tabs>
    </div>
  )
}

export default WorkspaceTabsWrapper
