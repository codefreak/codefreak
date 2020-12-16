import {
  EvaluationStepStatus,
  useEvaluationStatusUpdatedSubscription,
  useGetLatestEvaluationQuery
} from '../generated/graphql'
import { useEffect, useState } from 'react'

const useEvaluationStatus = (answerId: string) => {
  const [status, setStatus] = useState(EvaluationStepStatus.Pending)
  const { data: initialData } = useGetLatestEvaluationQuery({
    variables: { answerId }
  })
  useEffect(() => {
    if (initialData?.answer.latestEvaluation?.stepsStatusSummary) {
      setStatus(initialData?.answer.latestEvaluation?.stepsStatusSummary)
    }
  }, [initialData])

  const { data: pushData } = useEvaluationStatusUpdatedSubscription({
    variables: { answerId }
  })
  useEffect(() => {
    if (pushData?.evaluationStatusUpdated.status) {
      setStatus(pushData.evaluationStatusUpdated.status)
    }
  }, [pushData])

  return status
}

export default useEvaluationStatus
