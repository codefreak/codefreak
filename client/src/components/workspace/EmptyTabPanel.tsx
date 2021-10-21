import TabPanel from './TabPanel'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import React from 'react'
import useWorkspace from '../../hooks/workspace/useWorkspace'

export class EmptyWorkspaceTab extends WorkspaceTab {
  constructor() {
    super(WorkspaceTabType.EMPTY, '')
  }

  renderTitle(): React.ReactNode {
    return 'No files open'
  }

  renderContent(): React.ReactNode {
    return <EmptyTabPanel />
  }
}

const EmptyTabPanel = () => {
  const { isAvailable } = useWorkspace()

  return <TabPanel loading={!isAvailable} />
}

export default EmptyTabPanel
