import Button, { ButtonProps } from 'antd/lib/button'
import React, { useEffect, useState } from 'react'
import {
  PendingEvaluationStatus,
  useGetPendingEvaluationQuery,
  usePendingEvaluationUpdatedSubscription,
  useStartEvaluationMutation
} from '../services/codefreak-api'

interface StartEvaluationButtonProps extends ButtonProps {
  answerId: string
}

const StartEvaluationButton: React.FC<StartEvaluationButtonProps> = props => {
  const { answerId, ...restProps } = props

  const [status, setStatus] = useState(PendingEvaluationStatus.Finished)

  const pendingEvaluation = useGetPendingEvaluationQuery({
    variables: { answerId }
  })

  usePendingEvaluationUpdatedSubscription({
    variables: { answerId },
    onSubscriptionData: data => {
      if (data.subscriptionData.data) {
        setStatus(data.subscriptionData.data.pendingEvaluationUpdated.status)
      }
    }
  })

  const [start, startResult] = useStartEvaluationMutation({
    variables: { answerId }
  })

  useEffect(() => {
    if (
      pendingEvaluation.data &&
      pendingEvaluation.data.answer.pendingEvaluation
    ) {
      setStatus(pendingEvaluation.data.answer.pendingEvaluation.status)
      // if there is no pending evaluation, stay at 'finished'
    }
  }, [setStatus, pendingEvaluation.data])

  return (
    <Button
      icon="caret-right"
      onClick={start as () => void}
      loading={startResult.loading}
      disabled={
        pendingEvaluation.loading || status !== PendingEvaluationStatus.Finished
      }
      {...restProps}
    >
      Start Evaluation
    </Button>
  )
}

export default StartEvaluationButton
