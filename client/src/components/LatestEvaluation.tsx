import React from 'react'
import useAnswerEvaluation from '../hooks/useAnswerEvaluation'
import { Alert, Icon, Result, Spin } from 'antd'
import EvaluationResult from './EvaluationResult'
import StartEvaluationButton from './StartEvaluationButton'

export interface LatestEvaluationProps {
  answerId: string
  showTrigger: boolean
}

const LatestEvaluation: React.FC<LatestEvaluationProps> = props => {
  const { answerId, showTrigger } = props
  const { latestEvaluation, loading } = useAnswerEvaluation(answerId)

  if (loading) {
    return (
      <div style={{ textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!latestEvaluation) {
    if (showTrigger) {
      return (
        <Result
          icon={<Icon type="rocket" theme="twoTone" />}
          title="Wondering if your solution is correct? ✨"
          extra={
            <StartEvaluationButton
              answerId={answerId}
              type="primary"
              size="large"
            />
          }
        />
      )
    } else {
      return <Alert type="info" message="Answer has not been evaluated, yet" />
    }
  }

  return <EvaluationResult evaluationId={latestEvaluation.id} />
}

export default LatestEvaluation
