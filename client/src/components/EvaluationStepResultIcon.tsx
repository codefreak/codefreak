import { Icon, Popover } from 'antd'
import React from 'react'
import {
  EvaluationStep,
  EvaluationStepResult as Result
} from '../generated/graphql'

import './EvaluationStepResultIcon.less'

const EvaluationStepResultIcon: React.FC<{
  stepResult: Pick<EvaluationStep, 'result' | 'runnerName' | 'summary'>
}> = ({ stepResult: { result, runnerName, summary } }) => {
  let icon = <Icon type="exclamation-circle" />
  if (result === Result.Success) {
    icon = <Icon type="check-circle" />
  } else if (result === Result.Errored) {
    icon = <Icon type="close-circle" />
  }
  return (
    <Popover
      title={runnerName}
      content={summary}
      className="evaluation-step-result-icon"
    >
      {icon}
    </Popover>
  )
}

export default EvaluationStepResultIcon
