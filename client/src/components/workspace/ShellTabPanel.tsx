import useWorkspace from '../../hooks/workspace/useWorkspace'
import React, { useEffect } from 'react'
import TabPanel from './TabPanel'
import 'xterm/css/xterm.css'
import useStartProcessMutation from '../../hooks/workspace/useStartProcessMutation'
import AbstractProcessTabPanel from './AbstractProcessTabPanel'
import { ProcessType } from '../../hooks/workspace/useResizeProcessMutation'
import { WorkspaceTab, WorkspaceTabType } from '../../services/workspace-tabs'
import { CodeOutlined } from '@ant-design/icons'

export class ShellWorkspaceTab extends WorkspaceTab {
  constructor() {
    super(WorkspaceTabType.SHELL, '')
  }

  renderTitle(): React.ReactNode {
    return (
      <>
        <CodeOutlined /> Shell
      </>
    )
  }

  renderContent(): React.ReactNode {
    return <ShellTabPanel />
  }
}

const ShellTabPanel = () => {
  const { baseUrl } = useWorkspace()
  const {
    mutate: startProcess,
    data: processId,
    isIdle
  } = useStartProcessMutation()

  useEffect(() => {
    if (!processId && baseUrl.length > 0 && isIdle) {
      startProcess()
    }
  }, [baseUrl, isIdle, processId, startProcess])

  if (!processId) {
    return <TabPanel withPadding loading />
  }

  return (
    <AbstractProcessTabPanel processId={processId} type={ProcessType.SHELL} />
  )
}

export default ShellTabPanel
