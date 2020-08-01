import {
  AssignmentStatus,
  useAssignmentStatusChangeSubscription
} from '../generated/graphql'

const useAssignmentStatusChange = (
  assignmentId: string | undefined,
  onChange: (newStatus: AssignmentStatus) => void
) => {
  useAssignmentStatusChangeSubscription({
    variables: { assignmentId: assignmentId || '' },
    skip: assignmentId === undefined,
    onSubscriptionData: ({ subscriptionData: { data } }) => {
      const newStatus = data?.assignmentStatusChange
      if (newStatus) {
        onChange(newStatus)
      }
    }
  })
}

export default useAssignmentStatusChange
