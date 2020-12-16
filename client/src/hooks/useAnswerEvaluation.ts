import { useEffect, useState } from 'react'
import {
  EvaluationStepStatus,
  LatestEvaluationFragment,
  useGetLatestEvaluationLazyQuery
} from '../generated/graphql'
import useEvaluationStatus from './useEvaluationStatus'

const useAnswerEvaluation = (
  answerId: string,
  initialLatestEvaluation: LatestEvaluationFragment | undefined | null
) => {
  const evaluationStatus = useEvaluationStatus(answerId)

  const [latestEvaluation, setLatestEvaluation] = useState<
    LatestEvaluationFragment | null | undefined
  >(initialLatestEvaluation)
  const [
    fetchLatestEvaluation,
    latestEvaluationQuery
  ] = useGetLatestEvaluationLazyQuery({
    variables: { answerId },
    fetchPolicy: 'network-only'
  })

  useEffect(() => {
    if (evaluationStatus === EvaluationStepStatus.Finished) {
      fetchLatestEvaluation()
    }
  }, [evaluationStatus, fetchLatestEvaluation])

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
    evaluationStatus,
    loading: latestEvaluationQuery.loading
  }
}

export default useAnswerEvaluation
