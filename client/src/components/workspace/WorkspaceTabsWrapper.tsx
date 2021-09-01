import { Tabs } from 'antd'
import { extractRelativeFilePath, readFilePath } from '../../services/workspace'
import useWorkspace from '../../hooks/useWorkspace'
import EditorTabPanel from './EditorTabPanel'

export const FIXED_FILE_NAME = 'main.py'

export enum WorkspaceTabType {
  EDITOR,
  EMPTY
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
    default:
      return ''
  }
}

type WorkspaceTabsWrapperProps = {
  baseUrl: string
}

const WorkspaceTabsWrapper = ({ baseUrl }: WorkspaceTabsWrapperProps) => {
  const { isWorkspaceAvailable } = useWorkspace(baseUrl)

  const filePath = readFilePath(baseUrl, FIXED_FILE_NAME)
  const title = getTabTitle(
    WorkspaceTabType.EDITOR,
    filePath,
    !isWorkspaceAvailable
  )

  return (
    <div className="workspace-tabs-wrapper">
      <Tabs hideAdd type="card" className="workspace-tabs">
        <Tabs.TabPane tab={title} key={title} />
      </Tabs>
      <EditorTabPanel baseUrl={baseUrl} />
    </div>
  )
}

export default WorkspaceTabsWrapper
