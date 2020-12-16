import { Badge } from 'antd'
import React from 'react'
import useLatestEvaluation from '../hooks/useLatestEvaluation'
import { EvaluationErrorIcon } from './Icons'
import useEvaluationStatus from '../hooks/useEvaluationStatus'
import { EvaluationStepStatus } from '../generated/graphql'

interface EvaluationIndicatorProps {
  style?: React.CSSProperties
  answerId: string
}

const EvaluationIndicator: React.FC<EvaluationIndicatorProps> = props => {
  const { answerId, style } = props
  const latest = useLatestEvaluation(answerId)
  const status = useEvaluationStatus(answerId)

  if (
    status === EvaluationStepStatus.Running ||
    status === EvaluationStepStatus.Queued
  ) {
    return <Badge style={style} status="processing" />
  }

  if (latest.summary === 'SUCCESS') {
    return <Badge style={style} status="success" />
  }

  if (latest.summary === 'FAILED') {
    return <Badge style={style} status="error" />
  }

  if (latest.summary === 'ERRORED') {
    return <EvaluationErrorIcon style={style} />
  }

  return <Badge style={style} status="default" />
}

export default EvaluationIndicator
