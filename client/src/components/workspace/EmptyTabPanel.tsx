import TabPanel, { TabPanelProps } from './TabPanel'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import React from 'react'

export class EmptyWorkspaceTab extends WorkspaceTab {
  constructor() {
    super(WorkspaceTabType.EMPTY, '')
  }

  renderTitle(): React.ReactNode {
    return 'No files open'
  }

  renderContent(loading: boolean): React.ReactNode {
    return <EmptyTabPanel loading={loading} />
  }
}

const EmptyTabPanel = ({ loading }: TabPanelProps) => (
  <TabPanel loading={loading} />
)

export default EmptyTabPanel
