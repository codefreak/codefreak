import React from 'react'
import { EvaluationStepStatus } from '../generated/graphql'
import { Icon } from 'antd'

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
  const className = `evaluation-step-status-icon-${status.toLowerCase()}`

  return (
    <Icon
      type={iconType}
      className={`evaluation-step-status-icon ${className}`}
    />
  )
}

export default EvaluationStepStatusIcon
