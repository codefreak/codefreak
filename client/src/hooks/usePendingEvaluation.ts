import { useEffect, useState } from 'react'
import {
  PendingEvaluationStatus,
  useGetPendingEvaluationQuery
} from '../services/codefreak-api'
import { noop } from '../services/util'
import usePendingEvaluationUpdated from './usePendingEvaluationUpdated'

const usePendingEvaluation = (
  answerId: string,
  onFinish: () => void = noop
): { status: PendingEvaluationStatus | null; loading: boolean } => {
  const [status, setStatus] = useState<PendingEvaluationStatus | null>(null)

  const pendingEvaluation = useGetPendingEvaluationQuery({
    variables: { answerId },
    fetchPolicy: 'network-only'
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

  usePendingEvaluationUpdated(answerId, newStatus => {
    if (newStatus === 'FINISHED' && status !== 'FINISHED') {
      onFinish()
    }
    setStatus(newStatus)
  })

  return { status, loading: pendingEvaluation.loading }
}

export default usePendingEvaluation
