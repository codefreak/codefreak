import {Grade, useGetGradeQuery} from "../generated/graphql";
import {  useEffect, useState } from 'react'


const useGetGrade =(
  evaluationId : string
) : { grade : Grade | null } =>{

  const [grade,setGrade] = useState<Grade | null>(null)

  const result = useGetGradeQuery({
    variables: {evaluationId}
  })
  useEffect(()=>{
    if(result.data?.grade){
      setGrade(result.data.grade)
      }
    },[result.data])



  return {grade}
}

export default useGetGrade
