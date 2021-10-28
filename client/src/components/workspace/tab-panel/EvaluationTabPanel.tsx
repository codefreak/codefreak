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
    // Don't query for the evaluation status while there is no answer-id
    const indicator =
      this.answerId.length > 0 ? (
        <EvaluationIndicator
          answerId={this.answerId}
          style={{ marginLeft: 8 }}
        />
      ) : null

    return (
      <>
        <DashboardOutlined /> Evaluation-Results {indicator}
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
    <TabPanel withPadding loading={answerId.length === 0}>
      <EvaluationPage answerId={answerId} />
    </TabPanel>
  )
}

export default EvaluationTabPanel
