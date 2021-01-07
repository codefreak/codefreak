import React from 'react'
import { EvaluationStepStatus } from '../generated/graphql'
import { Icon } from 'antd'
import { isEvaluationInProgress } from '../services/evaluation'
import EvaluationProcessingIcon from './EvaluationProcessingIcon'

const stepStatusIconMap: Record<EvaluationStepStatus, string> = {
  [EvaluationStepStatus.Pending]: 'question-circle',
  [EvaluationStepStatus.Queued]: 'loading',
  [EvaluationStepStatus.Running]: 'loading',
  [EvaluationStepStatus.Finished]: 'check-circle',
  [EvaluationStepStatus.Canceled]: 'close-circle'
}

interface EvaluationStepStatusIconProps {
  status: EvaluationStepStatus
}

const EvaluationStepStatusIcon: React.FC<EvaluationStepStatusIconProps> = props => {
  const { status } = props

  const iconType = stepStatusIconMap[status]
  const className = `evaluation-step-status-icon evaluation-step-status-icon-${status.toLowerCase()}`

  if (isEvaluationInProgress(status)) {
    return <EvaluationProcessingIcon className={className} />
  }

  return <Icon type={iconType} className={className} />
}

export default EvaluationStepStatusIcon
