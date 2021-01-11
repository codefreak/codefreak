import React from 'react'
import {
  EvaluationStepResult,
  EvaluationStepStatus
} from '../generated/graphql'
import EvaluationStepResultIcon from './EvaluationStepResultIcon'
import EvaluationStepStatusIcon from './EvaluationStepStatusIcon'

interface EvaluationStepIconProps {
  status: EvaluationStepStatus
  result?: EvaluationStepResult
}

const EvaluationStepIcon: React.FC<EvaluationStepIconProps> = props => {
  const { status, result } = props
  if (status === EvaluationStepStatus.Finished && result) {
    return <EvaluationStepResultIcon result={result} />
  }
  return <EvaluationStepStatusIcon status={status} />
}

export default EvaluationStepIcon
