import { EvaluationStepStatus } from '../generated/graphql'
import useAnswerEvaluation from './useAnswerEvaluation'

const useEvaluationStatus = (
  answerId: string,
  initialStatus?: EvaluationStepStatus
) => {
  const { evaluationStatus } = useAnswerEvaluation(answerId)
  return evaluationStatus || initialStatus
}

export default useEvaluationStatus
