import {
  EvaluationStepStatus,
  useEvaluationStatusUpdatedSubscription
} from '../services/codefreak-api'

const useEvaluationStatusUpdated = (
  answerId: string,
  onPendingEvaluationUpdated: (newStatus: EvaluationStepStatus) => void
) => {
  useEvaluationStatusUpdatedSubscription({
    variables: { answerId },
    onSubscriptionData: data => {
      if (data.subscriptionData.data) {
        onPendingEvaluationUpdated(
          data.subscriptionData.data.evaluationStatusUpdated.status
        )
      }
    }
  })
}

export default useEvaluationStatusUpdated
