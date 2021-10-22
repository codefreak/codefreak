import TabPanel from './TabPanel'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import React from 'react'

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
  return <TabPanel />
}

export default EmptyTabPanel
