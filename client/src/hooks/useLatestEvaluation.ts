import { useCallback, useEffect, useState } from 'react'
import {
  EvaluationStepResult,
  useEvaluationFinishedSubscription,
  useGetLatestEvaluationQuery
} from '../services/codefreak-api'

const useLatestEvaluation = (
  answerId: string
): { summary: EvaluationStepResult | null; loading: boolean } => {
  const [summary, setSummary] = useState<EvaluationStepResult | null>(null)

  const latestEvaluation = useGetLatestEvaluationQuery({
    variables: { answerId },
    fetchPolicy: 'cache-and-network'
  })

  useEffect(() => {
    if (
      latestEvaluation.data &&
      latestEvaluation.data.answer.latestEvaluation
    ) {
      setSummary(
        latestEvaluation.data.answer.latestEvaluation.stepsResultSummary
      )
      // if there is no latest evaluation, stay at null
    }
  }, [setSummary, latestEvaluation.data])

  useEvaluationFinishedSubscription({
    onSubscriptionData: useCallback(
      data => {
        if (
          data.subscriptionData.data?.evaluationFinished?.answer?.id ===
          answerId
        ) {
          setSummary(
            data.subscriptionData.data.evaluationFinished.stepsResultSummary
          )
        }
      },
      [answerId]
    )
  })

  return { summary, loading: latestEvaluation.loading }
}

export default useLatestEvaluation
