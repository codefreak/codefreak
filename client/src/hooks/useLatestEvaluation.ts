import { useEffect, useState } from 'react'
import {
  EvaluationStepResult,
  useEvaluationFinishedSubscription,
  useGetLatestEvaluationStatusQuery
} from '../services/codefreak-api'

const useLatestEvaluation = (
  answerId: string
): { summary: EvaluationStepResult | null; loading: boolean } => {
  const [summary, setSummary] = useState<EvaluationStepResult | null>(null)

  const latestEvaluation = useGetLatestEvaluationStatusQuery({
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
    onSubscriptionData: data => {
      if (data.subscriptionData.data) {
        setSummary(
          data.subscriptionData.data.evaluationFinished.stepsResultSummary
        )
      }
    }
  })

  return { summary, loading: latestEvaluation.loading }
}

export default useLatestEvaluation
