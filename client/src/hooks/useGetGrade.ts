import {EvaluationStepStatus, Grade, useGetGradeQuery} from "../generated/graphql";
import {useEffect, useState} from 'react'


const useGetGrade = (
  evaluationId: string,
  evaluationStepStatus?:EvaluationStepStatus
): { grade: Grade | undefined, fetchGrade: any } => {

  const result = useGetGradeQuery({
    variables: {evaluationId}
  })

  const [grade, setGrade] = useState<Grade | undefined>(undefined)

  useEffect(() => {
    if(result.data?.grade!=null || result.data?.grade!==undefined){
      setGrade(result.data.grade)
    }
  },[result])

  if(evaluationStepStatus!=null) {
    useEffect(()=>{
      if(evaluationStepStatus == EvaluationStepStatus.Finished){
        result.refetch()
      }
    },[evaluationStepStatus])
  }

  function fetchGrade(){
    return result.refetch()
  }


  return {grade,fetchGrade}
}

export default useGetGrade
