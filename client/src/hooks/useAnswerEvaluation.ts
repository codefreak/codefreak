import { useEffect, useState } from 'react'
import {
  LatestEvaluationFragment,
  EvaluationStepStatus,
  useGetLatestEvaluationStatusLazyQuery
} from '../generated/graphql'
import useEvaluationStatusUpdated from './useEvaluationStatusUpdated'

const useAnswerEvaluation = (
  answerId: string,
  initialLatestEvaluation: LatestEvaluationFragment | undefined | null
) => {
  const [pendingEvaluationStatus, setPendingEvaluationStatus] = useState<
    EvaluationStepStatus | null | undefined
  >(initialLatestEvaluation?.stepsStatusSummary)

  useEvaluationStatusUpdated(answerId, newStatus => {
    setPendingEvaluationStatus(newStatus)
    // start fetching new evaluation results if status changed to finished
    if (newStatus === EvaluationStepStatus.Finished) {
      fetchLatestEvaluation()
    }
  })

  const [latestEvaluation, setLatestEvaluation] = useState<
    LatestEvaluationFragment | null | undefined
  >(initialLatestEvaluation)
  const [
    fetchLatestEvaluation,
    latestEvaluationQuery
  ] = useGetLatestEvaluationStatusLazyQuery({
    variables: { answerId },
    fetchPolicy: 'network-only'
  })

  useEffect(() => {
    if (
      latestEvaluationQuery.data &&
      latestEvaluationQuery.data.answer.latestEvaluation
    ) {
      setLatestEvaluation(latestEvaluationQuery.data.answer.latestEvaluation)
    }
  }, [setLatestEvaluation, latestEvaluationQuery, latestEvaluationQuery.data])

  return {
    latestEvaluation,
    pendingEvaluationStatus,
    loading: latestEvaluationQuery.loading
  }
}

export default useAnswerEvaluation
