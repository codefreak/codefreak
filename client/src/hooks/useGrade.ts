import {
  EvaluationStepStatus,
  Grade,
  useGetGradeForEvaluationQuery
} from '../generated/graphql'
import { useCallback, useEffect, useState } from 'react'

const useGrade = (
  evaluationId: string,
  evaluationStatus?: EvaluationStepStatus
): { grade: Grade | undefined; fetchGrade(): void } => {
  // Query to get a Grade
  const { data, refetch } = useGetGradeForEvaluationQuery({
    variables: { evaluationId }
  })

  const [evaluationStatusState, setEvaluationStatusState] = useState<
    EvaluationStepStatus | undefined
  >(evaluationStatus)
  useEffect(() => {
    if (evaluationStatus !== undefined) {
      setEvaluationStatusState(evaluationStatus)
    }
  }, [evaluationStatus])

  // state Hook to manage a grade
  const [grade, setGrade] = useState<Grade | undefined>(undefined)

  const callback = useCallback(() => {
    if (evaluationStatusState === EvaluationStepStatus.Finished && refetch!==undefined) {
      refetch().finally()
      if (data !== null && data?.gradeForEvaluation !== null) {
        setGrade(data?.gradeForEvaluation)
      }
    }
  }, [evaluationStatusState, data, refetch])

  // Use callback function to prevent infinite-loop
  useEffect(() => {
    callback()
  }, [callback])

  // this exported function will update a grade if the related points are changed.
  const fetchGrade = (): void => {
    if(refetch!==undefined){
      refetch().finally()
    }
  }

  return { grade, fetchGrade }
}

export default useGrade
