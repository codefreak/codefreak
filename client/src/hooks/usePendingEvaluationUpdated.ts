import {
  PendingEvaluationStatus,
  usePendingEvaluationUpdatedSubscription
} from '../services/codefreak-api'

const usePendingEvaluationUpdated = (
  answerId: string,
  onPendingEvaluationUpdated: (newStatus: PendingEvaluationStatus) => void
) => {
  usePendingEvaluationUpdatedSubscription({
    variables: { answerId },
    onSubscriptionData: data => {
      if (data.subscriptionData.data) {
        onPendingEvaluationUpdated(
          data.subscriptionData.data.pendingEvaluationUpdated.status
        )
      }
    }
  })
}

export default usePendingEvaluationUpdated
