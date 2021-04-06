import React from 'react'
import useLiveAnswerEvaluation from '../hooks/useLiveAnswerEvaluation'
import { Alert, Result } from 'antd'
import EvaluationResult from './EvaluationResult'
import StartEvaluationButton from './StartEvaluationButton'
import LoadingIndicator from './LoadingIndicator'
import { RocketTwoTone } from '@ant-design/icons'
import useHasAuthority from '../hooks/useHasAuthority'

export interface LatestEvaluationProps {
  answerId: string
  showTrigger: boolean
}

const LatestEvaluation: React.FC<LatestEvaluationProps> = props => {
  const { answerId, showTrigger } = props
  const {
    latestEvaluation,
    evaluationStatus,
    loading
  } = useLiveAnswerEvaluation(answerId)

  const teacherAuthority = useHasAuthority('ROLE_TEACHER')

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

  return (
    <EvaluationResult
      evaluationId={latestEvaluation.id}
      evaluationStatus={evaluationStatus}
      teacherAuthority={teacherAuthority}
    />
  )
}

export default LatestEvaluation
