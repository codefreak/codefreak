import useWorkspace from '../../hooks/workspace/useWorkspace'
import TabPanel from './TabPanel'
import AbstractProcessTabPanel from './AbstractProcessTabPanel'
import { ProcessType } from '../../hooks/workspace/useResizeProcessMutation'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import { CaretRightOutlined } from '@ant-design/icons'

export class ConsoleWorkspaceTab extends WorkspaceTab {
  constructor() {
    super(WorkspaceTabType.CONSOLE, '')
  }

  renderTitle(): React.ReactNode {
    return (
      <>
        <CaretRightOutlined /> Console
      </>
    )
  }

  renderContent(): React.ReactNode {
    return <ConsoleTabPanel />
  }
}

const ConsoleTabPanel = () => {
  const { runProcessId } = useWorkspace()

  if (!runProcessId) {
    return <TabPanel withPadding loading />
  }

  return (
    <AbstractProcessTabPanel
      processId={runProcessId}
      type={ProcessType.CONSOLE}
    />
  )
}

export default ConsoleTabPanel
