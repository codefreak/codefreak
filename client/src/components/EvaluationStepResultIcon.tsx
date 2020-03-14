import { Icon } from 'antd'
import React from 'react'
import { EvaluationStepResult } from '../generated/graphql'

import './EvaluationStepResultIcon.less'

const EvaluationStepResultIcon: React.FC<{
  stepResult?: EvaluationStepResult | null
}> = ({ stepResult }) => {
  let iconType = 'exclamation-circle'
  if (stepResult === EvaluationStepResult.Success) {
    iconType = 'check-circle'
  } else if (stepResult === EvaluationStepResult.Errored) {
    iconType = 'close-circle'
  }

  return <Icon type={iconType} className="evaluation-step-result-icon" />
}

export default EvaluationStepResultIcon
