import {Grade, useGetGradeQuery} from "../../generated/graphql";
import React from "react";
import './GradeView.less'
import useGetGrade from "../../hooks/useGetGrade";

const GradeView : React.FC<{
  evaluationId:string

}>=props=>{

  const evaluationId = props.evaluationId
  const result = useGetGrade(evaluationId)

  if(result==null) return (<></>)

  if(result.grade!=null){
  const grade = result.grade
  if(grade.calculated){
    console.log("calculated? " + grade)
    if(grade.gradePercentage)
    return (<div className="grade-view">{(Math.round(grade.gradePercentage * 100) / 100).toFixed(2)}% Grade</div>)
    }
  }
  return (<></>)

}

export default GradeView

