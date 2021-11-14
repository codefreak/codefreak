import TabPanel from './TabPanel'
import {
  WorkspaceTab,
  WorkspaceTabType
} from '../../../services/workspace-tabs'
import React from 'react'

/**
 * Renders an EmptyTabPanel. The title will be 'No files open'
 */
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

/**
 * Renders a tab panel with no content
 */
const EmptyTabPanel = () => {
  return <TabPanel />
}

export default EmptyTabPanel
