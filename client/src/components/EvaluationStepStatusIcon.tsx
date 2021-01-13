import React from 'react'
import { EvaluationStepStatus } from '../generated/graphql'
import {
  QuestionCircleOutlined,
  LoadingOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import { isEvaluationInProgress } from '../services/evaluation'
import EvaluationProcessingIcon from './EvaluationProcessingIcon'

const stepStatusIconMap: Record<EvaluationStepStatus, React.ElementType> = {
  [EvaluationStepStatus.Pending]: QuestionCircleOutlined,
  [EvaluationStepStatus.Queued]: LoadingOutlined,
  [EvaluationStepStatus.Running]: LoadingOutlined,
  [EvaluationStepStatus.Finished]: CheckCircleOutlined,
  [EvaluationStepStatus.Canceled]: CheckCircleOutlined
}

interface EvaluationStepStatusIconProps {
  status: EvaluationStepStatus
}

const EvaluationStepStatusIcon: React.FC<EvaluationStepStatusIconProps> = props => {
  const { status } = props

  const className = `evaluation-step-status-icon evaluation-step-status-icon-${status.toLowerCase()}`

  if (isEvaluationInProgress(status)) {
    return <EvaluationProcessingIcon className={className} />
  }

  const IconType = stepStatusIconMap[status]
  return <IconType className={className} />
}

export default EvaluationStepStatusIcon
