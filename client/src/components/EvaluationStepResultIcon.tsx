import React from 'react'
import { EvaluationStepResult } from '../generated/graphql'

import './EvaluationStepResultIcon.less'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons'

const EvaluationStepResultIcon: React.FC<{
  stepResult?: EvaluationStepResult | null
}> = ({ stepResult }) => {
  let icon = <QuestionCircleOutlined className="evaluation-step-result-icon" />
  if (stepResult === EvaluationStepResult.Success) {
    icon = <CheckCircleOutlined className="evaluation-step-result-icon" />
  } else if (stepResult === EvaluationStepResult.Errored) {
    icon = <CloseCircleOutlined className="evaluation-step-result-icon" />
  } else if (stepResult === EvaluationStepResult.Failed) {
    icon = <ExclamationCircleOutlined className="evaluation-step-result-icon" />
  }

  return icon
}

export default EvaluationStepResultIcon
