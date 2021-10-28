import TabPanel from './TabPanel'
import EvaluationPage from '../../../pages/evaluation/EvaluationOverviewPage'
import useWorkspace from '../../../hooks/workspace/useWorkspace'
import {
  WorkspaceTab,
  WorkspaceTabType
} from '../../../services/workspace-tabs'
import { DashboardOutlined } from '@ant-design/icons'
import EvaluationIndicator from '../../EvaluationIndicator'
import React from 'react'

export class EvaluationWorkspaceTab extends WorkspaceTab {
  private readonly answerId: string

  constructor(answerId: string) {
    super(WorkspaceTabType.EVALUATION, '')
    this.answerId = answerId
  }

  renderTitle(): React.ReactNode {
    return (
      <>
        <DashboardOutlined /> Evaluation-Results{' '}
        <EvaluationIndicator
          answerId={this.answerId}
          style={{ marginLeft: 8 }}
        />
      </>
    )
  }

  renderContent(): React.ReactNode {
    return <EvaluationTabPanel />
  }
}

const EvaluationTabPanel = () => {
  const { answerId } = useWorkspace()
  return (
    <TabPanel withPadding>
      <EvaluationPage answerId={answerId} />
    </TabPanel>
  )
}

export default EvaluationTabPanel
