import {
  EvaluationStepStatus,
  Grade,
  useGetGradeForEvaluationQuery
} from '../generated/graphql'
import { useCallback, useEffect, useState } from 'react'
import { messageService } from '../services/message'

const useGrade = (
  evaluationId: string,
  evaluationStatus?: EvaluationStepStatus
): { grade: Grade | undefined; fetchGrade(): void } => {
  // Query to get a Grade
  const result = useGetGradeForEvaluationQuery({
    variables: { evaluationId }
  })

  const [update, setUpdate] = useState<boolean>(true)

  // state Hook to manage a grade
  const [grade, setGrade] = useState<Grade | undefined>(undefined)

  const callback = useCallback(() => {
    if (evaluationStatus === EvaluationStepStatus.Finished) {
      messageService.success('Grade Calculated')
    }
    result.refetch()
  }, [result, evaluationStatus])

  // Set a Grade in a state Hook if one exists.
  useEffect(() => {
    if (result.data !== null && result.data !== undefined) {
      if (update) {
        setGrade(result.data?.gradeForEvaluation!!)
        setUpdate(false)
      }
    }
  }, [result, update, grade])

  // this exported function will update a grade if the related points are changed.
  const fetchGrade = (): void => {
    callback()
    // Async Sleep timer. Wait 500ms before updating the grade over useEffect.
    new Promise(resolve => setTimeout(resolve, 500)).finally(() =>
      setUpdate(true)
    )
  }

  return { grade, fetchGrade }
}

export default useGrade
