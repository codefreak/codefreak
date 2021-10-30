import TabPanel from './TabPanel'
import { renderTaskInstructionsText } from '../../../pages/task/TaskConfigurationPage'
import { useGetTaskDetailsQuery } from '../../../services/codefreak-api'
import useWorkspace from '../../../hooks/workspace/useWorkspace'
import {
  WorkspaceTab,
  WorkspaceTabType
} from '../../../services/workspace-tabs'
import { SolutionOutlined } from '@ant-design/icons'
import React from 'react'

export class InstructionsWorkspaceTab extends WorkspaceTab {
  constructor() {
    super(WorkspaceTabType.INSTRUCTIONS, '')
  }

  renderTitle(): React.ReactNode {
    return (
      <>
        <SolutionOutlined /> Instructions
      </>
    )
  }

  renderContent(): React.ReactNode {
    return <InstructionsTabPanel />
  }
}

const InstructionsTabPanel = () => {
  const { taskId: id } = useWorkspace()
  const { data } = useGetTaskDetailsQuery({
    variables: { id, teacher: false }
  })
  const instructions = data?.task.body ?? ''

  return (
    <TabPanel withPadding>{renderTaskInstructionsText(instructions)}</TabPanel>
  )
}

export default InstructionsTabPanel
