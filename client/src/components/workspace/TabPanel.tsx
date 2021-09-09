import React from 'react'
import { Card } from 'antd'

export const LoadingTabPanelPlaceholder = () => (
  <Card loading className="workspace-tab-panel-placeholder" />
)

export interface TabPanelProps {
  loading?: boolean
  withPadding?: boolean
}

const TabPanel = ({
  loading,
  withPadding = false,
  children
}: React.PropsWithChildren<TabPanelProps>) => {
  if (loading) {
    return <LoadingTabPanelPlaceholder />
  }

  return (
    <div
      className="workspace-tab-panel"
      style={{ padding: withPadding ? 16 : 0 }}
    >
      {children}
    </div>
  )
}

export default TabPanel
