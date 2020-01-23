import { useEffect, useState } from 'react'
import {
  PendingEvaluationStatus,
  useGetPendingEvaluationQuery,
  usePendingEvaluationUpdatedSubscription
} from '../services/codefreak-api'

const usePendingEvaluation = (
  answerId: string
): { status: PendingEvaluationStatus | null; loading: boolean } => {
  const [status, setStatus] = useState<PendingEvaluationStatus | null>(null)

  const pendingEvaluation = useGetPendingEvaluationQuery({
    variables: { answerId }
  })

  useEffect(() => {
    if (
      pendingEvaluation.data &&
      pendingEvaluation.data.answer.pendingEvaluation
    ) {
      setStatus(pendingEvaluation.data.answer.pendingEvaluation.status)
      // if there is no pending evaluation, stay at null
    }
  }, [setStatus, pendingEvaluation.data])

  usePendingEvaluationUpdatedSubscription({
    variables: { answerId },
    onSubscriptionData: data => {
      if (data.subscriptionData.data) {
        setStatus(data.subscriptionData.data.pendingEvaluationUpdated.status)
      }
    }
  })

  return { status, loading: pendingEvaluation.loading }
}

export default usePendingEvaluation
