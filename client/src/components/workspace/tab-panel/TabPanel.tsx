import React from 'react'
import { Card } from 'antd'

/**
 * Shows a card with a loading animation
 */
export const LoadingTabPanelPlaceholder = () => (
  <Card loading className="workspace-tab-panel-placeholder" />
)

/**
 * Provides a loading state and an option to have padding in the tab panel
 */
export interface TabPanelProps {
  /**
   * Whether the tab panel is in a loading state
   */
  loading?: boolean

  /**
   * Whether the tab panel should have padding
   */
  withPadding?: boolean

  /**
   * The children to render
   */
  children?: React.ReactNode
}

/**
 * A generic tab panel
 */
const TabPanel = React.forwardRef<HTMLDivElement, TabPanelProps>(
  ({ loading, withPadding = false, children }, ref) => {
    if (loading) {
      return <LoadingTabPanelPlaceholder />
    }

    return (
      <div
        className="workspace-tab-panel"
        ref={ref}
        style={{ padding: withPadding ? 16 : 0 }}
      >
        {children}
      </div>
    )
  }
)

export default TabPanel
