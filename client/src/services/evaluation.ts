import { EvaluationStepStatus } from '../generated/graphql'

export const isEvaluationInProgress = (status?: EvaluationStepStatus) => {
  return (
    status === EvaluationStepStatus.Running ||
    status === EvaluationStepStatus.Queued
  )
}
