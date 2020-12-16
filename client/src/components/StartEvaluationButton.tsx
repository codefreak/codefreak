import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import usePendingEvaluation from '../hooks/usePendingEvaluation'
import {
  EvaluationStepStatus,
  useStartEvaluationMutation
} from '../services/codefreak-api'

interface StartEvaluationButtonProps extends ButtonProps {
  answerId: string
}

const StartEvaluationButton: React.FC<StartEvaluationButtonProps> = props => {
  const { answerId, ...restProps } = props

  const { status, loading } = usePendingEvaluation(answerId)

  const [start, startResult] = useStartEvaluationMutation({
    variables: { answerId }
  })

  return (
    <Button
      icon="caret-right"
      onClick={start as () => void}
      loading={startResult.loading}
      disabled={
        loading ||
        status === EvaluationStepStatus.Queued ||
        status === EvaluationStepStatus.Running
      }
      {...restProps}
    >
      Start Evaluation
    </Button>
  )
}

export default StartEvaluationButton
