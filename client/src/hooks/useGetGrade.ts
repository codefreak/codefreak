import {Grade, useGetGradeQuery} from "../generated/graphql";
import {useEffect, useState} from 'react'


const useGetGrade = (
  evaluationId: string
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

  function fetchGrade(){
    return result.refetch()
  }


  return {grade,fetchGrade}
}

export default useGetGrade
