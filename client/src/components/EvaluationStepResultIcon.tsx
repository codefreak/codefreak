import { Icon } from 'antd'
import React from 'react'
import { EvaluationStepResult } from '../generated/graphql'

import './EvaluationStepResultIcon.less'

interface EvaluationStepResultIconProps {
  result: EvaluationStepResult
}

const EvaluationStepResultIcon: React.FC<EvaluationStepResultIconProps> = props => {
  const { result } = props
  let iconType = 'question-circle'
  if (result === EvaluationStepResult.Success) {
    iconType = 'check-circle'
  } else if (result === EvaluationStepResult.Errored) {
    iconType = 'close-circle'
  } else if (result === EvaluationStepResult.Failed) {
    iconType = 'exclamation-circle'
  }

  return <Icon type={iconType} className="evaluation-step-result-icon" />
}

export default EvaluationStepResultIcon
