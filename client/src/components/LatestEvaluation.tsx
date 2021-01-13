import React from 'react'
import useAnswerEvaluation from '../hooks/useAnswerEvaluation'
import { Alert, Result } from 'antd'
import EvaluationResult from './EvaluationResult'
import StartEvaluationButton from './StartEvaluationButton'
import LoadingIndicator from './LoadingIndicator'
import { RocketTwoTone } from '@ant-design/icons'

export interface LatestEvaluationProps {
  answerId: string
  showTrigger: boolean
}

const LatestEvaluation: React.FC<LatestEvaluationProps> = props => {
  const { answerId, showTrigger } = props
  const { latestEvaluation, loading } = useAnswerEvaluation(answerId)

  if (loading) {
    return <LoadingIndicator />
  }

  if (!latestEvaluation) {
    if (showTrigger) {
      return (
        <Result
          icon={<RocketTwoTone />}
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
