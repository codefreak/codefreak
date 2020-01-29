import { Badge, Icon } from 'antd'
import React from 'react'
import useLatestEvaluation from '../hooks/useLatestEvaluation'
import usePendingEvaluation from '../hooks/usePendingEvaluation'

interface EvaluationIndicatorProps {
  style?: React.CSSProperties
  answerId: string
}

const EvaluationIndicator: React.FC<EvaluationIndicatorProps> = props => {
  const { answerId, style } = props
  const latest = useLatestEvaluation(answerId)
  const pending = usePendingEvaluation(answerId)

  if (pending.status === 'RUNNING' || pending.status === 'QUEUED') {
    return <Badge style={style} status="processing" />
  }

  if (latest.summary === 'SUCCESS') {
    return <Badge style={style} status="success" />
  }

  if (latest.summary === 'FAILED') {
    return <Badge style={style} status="error" />
  }

  if (latest.summary === 'ERRORED') {
    return <Icon type="exclamation-circle" style={{ color: 'red', ...style }} />
  }

  return <Badge style={style} status="default" />
}

export default EvaluationIndicator
