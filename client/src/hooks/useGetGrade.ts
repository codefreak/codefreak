import {
  EvaluationStepStatus,
  Grade,
  useGetGradeQuery
} from '../generated/graphql'
import { useCallback, useEffect, useState } from 'react'

export interface FetchGrade{
  ():void
}

const useGetGrade = (
  evaluationId: string,
  evaluationStatus?: EvaluationStepStatus
): { grade: Grade | undefined; fetchGrade: FetchGrade } => {
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
  }, [evaluationStatus, memorizeCallback])

  let fetchGrade : FetchGrade
  fetchGrade=function ():void {
    result.refetch()
  }

  // function fetchGrade() {
  //   return result.refetch()
  // }

  return { grade, fetchGrade }
}

export default useGetGrade

