import { useEffect } from 'react'
import {
  EvaluationStatusUpdatedDocument,
  useGetLatestEvaluationQuery
} from '../generated/graphql'

/**
 * Get the latest evaluation for an answer and keep track of status updates
 * via subscription.
 *
 * @param answerId
 */
const useLiveAnswerEvaluation = (answerId: string) => {
  const latestEvaluationQuery = useGetLatestEvaluationQuery({
    variables: { answerId },
    fetchPolicy: 'network-only'
  })

  useEffect(() => {
    if (latestEvaluationQuery.data?.answer.latestEvaluation) {
      latestEvaluationQuery.subscribeToMore({
        document: EvaluationStatusUpdatedDocument,
        variables: { answerId },
        updateQuery: (previousQueryResult, { subscriptionData }) => {
          if (!subscriptionData.data) return previousQueryResult
          return subscriptionData.data
        }
      })
    }
  }, [latestEvaluationQuery.data])

  const latestEvaluation = latestEvaluationQuery.data?.answer.latestEvaluation
  const evaluationStatus = latestEvaluation?.stepsStatusSummary
  return {
    latestEvaluation,
    evaluationStatus,
    loading: latestEvaluationQuery.loading
  }
}

export default useLiveAnswerEvaluation
