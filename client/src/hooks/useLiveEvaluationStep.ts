import {
  EvaluationStepStatusUpdatedDocument,
  useGetEvaluationStepQuery
} from '../generated/graphql'
import { useEffect } from 'react'

const useLiveEvaluationStep = (stepId: string) => {
  const stepDetailsQuery = useGetEvaluationStepQuery({ variables: { stepId } })
  useEffect(() => {
    const stepData = stepDetailsQuery.data?.evaluationStep
    if (stepData) {
      stepDetailsQuery.subscribeToMore({
        document: EvaluationStepStatusUpdatedDocument,
        variables: { stepId },
        updateQuery: (prev, { subscriptionData }) => {
          if (!subscriptionData.data) return prev
          return subscriptionData.data
        }
      })
    }
  }, [stepId, stepDetailsQuery])

  return stepDetailsQuery
}

export default useLiveEvaluationStep
