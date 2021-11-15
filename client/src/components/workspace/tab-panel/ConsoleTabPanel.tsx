import useWorkspace from '../../../hooks/workspace/useWorkspace'
import TabPanel from './TabPanel'
import AbstractProcessTabPanel from './AbstractProcessTabPanel'
import {
  WorkspaceTab,
  WorkspaceTabType
} from '../../../services/workspace-tabs'
import { CaretRightOutlined } from '@ant-design/icons'

/**
 * Renders a ConsoleTabPanel
 */
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

/**
 * Renders a terminal for the current run process
 */
const ConsoleTabPanel = () => {
  const { runProcessId, isAvailable } = useWorkspace()

  if (!runProcessId) {
    return (
      <TabPanel withPadding loading={isAvailable}>
        Press "Run" to start the program!
      </TabPanel>
    )
  }

  return <AbstractProcessTabPanel processId={runProcessId} />
}

export default ConsoleTabPanel
