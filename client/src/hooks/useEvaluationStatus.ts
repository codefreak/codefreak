import { EvaluationStepStatus } from '../generated/graphql'
import useLiveAnswerEvaluation from './useLiveAnswerEvaluation'

const useEvaluationStatus = (
  answerId: string,
  initialStatus?: EvaluationStepStatus
) => {
  const { evaluationStatus } = useLiveAnswerEvaluation(answerId)
  return evaluationStatus || initialStatus
}

export default useEvaluationStatus
