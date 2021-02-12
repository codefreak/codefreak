import {  useEffect, useState } from 'react'
import {
  PointsOfEvaluationStep,
  useGetPointsOfEvaluationStepByEvaluationStepIdQuery,
  useUpdatePointsOfEvaluationStepMutation
} from "../generated/graphql";

const usePointsOfEvaluationStep = (
  evaluationStepId : string
) : { poe : PointsOfEvaluationStep | null} => {

  const [poe,setPoe] = useState<PointsOfEvaluationStep | null>(null)


  const {data : poeInput} = useGetPointsOfEvaluationStepByEvaluationStepIdQuery({
    variables: {evaluationStepId}
  })


  //Only Run on initial render
  useEffect(()=>{
    if(poeInput?.pointsOfEvaluationStepByEvaluationStepId){setPoe(poeInput.pointsOfEvaluationStepByEvaluationStepId)}
  },[poeInput])



  return {poe}
}



export default usePointsOfEvaluationStep

