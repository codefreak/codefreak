import { useEvaluationStatusUpdatedSubscription } from '../generated/graphql'

/**
 * Small wrapper around useEvaluationStatusUpdatedSubscription that returns
 * only the evaluation.
 *
 * @param answerId
 */
const useEvaluationStatusUpdates = (answerId: string) => {
  const { data } = useEvaluationStatusUpdatedSubscription({
    variables: { answerId }
  })

  return data?.evaluationStatusUpdated?.evaluation
}

export default useEvaluationStatusUpdates
