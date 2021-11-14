import React from 'react'
import { Tabs, Tooltip } from 'antd'
import { WorkspaceTabType, WorkspaceTab } from '../../services/workspace-tabs'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import { noop } from '../../services/util'
import { EmptyWorkspaceTab } from './tab-panel/EmptyTabPanel'

/**
 * Renders a single tab with its title and content
 */
const renderTab = (loading: boolean) => (tab: WorkspaceTab) => {
  const title = tab.renderTitle()
  const content = tab.renderContent()

  const key = tab.path.length > 0 ? tab.path : tab.type
  const isEditorTab = tab.type === WorkspaceTabType.EDITOR

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

/**
 * Provides the open tabs, the active tab and callbacks for when the active tab changes and when a tab is closed
 */
interface WorkspaceTabsWrapperProps {
  /**
   * The open tabs
   */
  tabs: WorkspaceTab[]

  /**
   * The active tab
   */
  activeTab?: string | WorkspaceTabType

  /**
   * Called when the active tab changes
   */
  onTabChange?: (activeKey: string) => void

  /**
   * Called when a tab is closed
   */
  onTabClose?: (key: string) => void
}

/**
 * Renders a tabs area with the given tabs
 */
const WorkspaceTabsWrapper = ({
  tabs,
  activeTab,
  onTabChange = noop,
  onTabClose = noop
}: WorkspaceTabsWrapperProps) => {
  const { isAvailable } = useWorkspace()

  const renderTabImpl = renderTab(!isAvailable)

  const renderedTabs =
    tabs.length > 0
      ? tabs.map(renderTabImpl)
      : renderTabImpl(new EmptyWorkspaceTab())

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
