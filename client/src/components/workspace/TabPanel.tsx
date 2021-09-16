import React from 'react'
import { Card } from 'antd'

export const LoadingTabPanelPlaceholder = () => (
  <Card loading className="workspace-tab-panel-placeholder" />
)

export interface TabPanelProps {
  loading?: boolean
  withPadding?: boolean
  children?: React.ReactNode
}

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
