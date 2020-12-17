import { useEffect, useState } from 'react'
import {
  LatestEvaluationFragment,
  useEvaluationStatusUpdatedSubscription,
  useGetLatestEvaluationQuery
} from '../generated/graphql'

const useAnswerEvaluation = (
  answerId: string,
  initialLatestEvaluation?: LatestEvaluationFragment
) => {
  const [latestEvaluation, setLatestEvaluation] = useState<
    LatestEvaluationFragment | undefined
  >(initialLatestEvaluation)

  // fetch the last evaluation if no initial data was supplied
  const latestEvaluationQuery = useGetLatestEvaluationQuery({
    variables: { answerId },
    fetchPolicy: 'network-only',
    skip: initialLatestEvaluation !== undefined
  })
  useEffect(() => {
    if (latestEvaluationQuery.data?.answer.latestEvaluation) {
      setLatestEvaluation(latestEvaluationQuery.data.answer.latestEvaluation)
    }
  }, [setLatestEvaluation, latestEvaluationQuery, latestEvaluationQuery.data])

  const { data: pushData } = useEvaluationStatusUpdatedSubscription({
    variables: { answerId }
  })
  useEffect(() => {
    if (pushData) {
      setLatestEvaluation(pushData?.evaluationStatusUpdated.evaluation)
    }
  }, [pushData])

  return {
    latestEvaluation,
    evaluationStatus: latestEvaluation?.stepsStatusSummary,
    loading: latestEvaluationQuery.loading
  }
}

export default useAnswerEvaluation
