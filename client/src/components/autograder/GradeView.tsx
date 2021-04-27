import { Grade } from '../../generated/graphql'
import React from 'react'
import './GradeView.less'

const GradeView: React.FC<{
  grade: Grade
}> = props => {
  const grade = props.grade
  if (
    grade !== null &&
    grade.gradePercentage !== undefined &&
    grade.gradePercentage !== null
  ) {
    return (
      <div className="grade-view">
        {(Math.round(grade.gradePercentage * 100) / 100).toFixed(2)}% Grade
      </div>
    )
  }
  return null
}
export default GradeView
