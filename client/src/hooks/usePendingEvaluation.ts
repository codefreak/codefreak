import { useEffect, useState } from 'react'
import {
  EvaluationStepStatus,
  useGetPendingEvaluationQuery
} from '../services/codefreak-api'
import { noop } from '../services/util'
import useEvaluationStatusUpdated from './useEvaluationStatusUpdated'

const usePendingEvaluation = (
  answerId: string,
  onFinish: () => void = noop
): { status: EvaluationStepStatus | null; loading: boolean } => {
  const [status, setStatus] = useState<EvaluationStepStatus | null>(null)

  const pendingEvaluation = useGetPendingEvaluationQuery({
    variables: { answerId },
    fetchPolicy: 'network-only'
  })

  useEffect(() => {
    if (
      pendingEvaluation.data &&
      pendingEvaluation.data.answer.latestEvaluation
    ) {
      setStatus(
        pendingEvaluation.data.answer.latestEvaluation.stepsStatusSummary
      )
      // if there is no pending evaluation, stay at null
    }
  }, [setStatus, pendingEvaluation.data])

  useEvaluationStatusUpdated(answerId, newStatus => {
    if (newStatus === 'FINISHED' && status !== 'FINISHED') {
      onFinish()
    }
    setStatus(newStatus)
  })

  return { status, loading: pendingEvaluation.loading }
}

export default usePendingEvaluation
