import React from 'react'
import { EvaluationStepResult } from '../generated/graphql'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'

import './EvaluationStepResultIcon.less'

const stepResultIconMap: Record<EvaluationStepResult, React.ElementType> = {
  [EvaluationStepResult.Success]: CheckCircleOutlined,
  [EvaluationStepResult.Failed]: ExclamationCircleOutlined,
  [EvaluationStepResult.Errored]: CloseCircleOutlined
}

interface EvaluationStepResultIconProps {
  result: EvaluationStepResult
}

const EvaluationStepResultIcon: React.FC<EvaluationStepResultIconProps> =
  props => {
    const { result } = props

    const IconType = stepResultIconMap[result]
    return <IconType className="evaluation-step-result-icon" />
  }

export default EvaluationStepResultIcon
