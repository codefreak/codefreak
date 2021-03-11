import {
  EvaluationStepStatus,
  Grade,
  useGetGradeQuery
} from '../generated/graphql'
import { useCallback, useEffect, useState } from 'react'

const useGetGrade = (
  evaluationId: string,
  evaluationStepStatus?: EvaluationStepStatus
): { grade: Grade | undefined; fetchGrade: any } => {
  const result = useGetGradeQuery({
    variables: { evaluationId }
  })

  const [grade, setGrade] = useState<Grade | undefined>(undefined)

  useEffect(() => {
    if (result.data?.grade !== null) {
      setGrade(result.data?.grade)
    }
  }, [result])

  const memorizeCallback = useCallback(() => {
    result.refetch()
  }, [result])

  useEffect(() => {
    memorizeCallback()
  }, [evaluationStepStatus, memorizeCallback])

  function fetchGrade() {
    return result.refetch()
  }

  return { grade, fetchGrade }
}

export default useGetGrade
