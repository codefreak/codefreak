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

  const latesEvaluation = useGetLatestEvaluationStatusQuery({
    variables: { answerId },
    fetchPolicy: 'cache-and-network'
  })

  useEffect(() => {
    if (latesEvaluation.data && latesEvaluation.data.answer.latestEvaluation) {
      setSummary(
        latesEvaluation.data.answer.latestEvaluation.stepsResultSummary
      )
      // if there is no latest evaluation, stay at null
    }
  }, [setSummary, latesEvaluation.data])

  useEvaluationFinishedSubscription({
    variables: { answerId },
    onSubscriptionData: data => {
      if (data.subscriptionData.data) {
        setSummary(
          data.subscriptionData.data.evaluationFinished.stepsResultSummary
        )
      }
    }
  })

  return { summary, loading: latesEvaluation.loading }
}

export default useLatestEvaluation
