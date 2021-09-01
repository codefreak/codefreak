import React from 'react'
import { Card } from 'antd'

export const LoadingTabPanelPlaceholder = () => (
  <Card loading className="workspace-tab-panel-placeholder" />
)

export interface TabPanelProps {
  loading?: boolean
}

const TabPanel = ({
  loading,
  children
}: React.PropsWithChildren<TabPanelProps>) => {
  if (loading) {
    return <LoadingTabPanelPlaceholder />
  }

  return <div className="workspace-tab-panel">{children}</div>
}

export default TabPanel
