import React from 'react'
import {
  EvaluationStepStatus,
  useGetDetailedEvaluatonQuery
} from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'
import './EvaluationResult.less'
import EvaluationStepPanel from './EvaluationStepPanel'
import useGrade from '../hooks/useGrade'
import GradeView from './autograder/GradeView'

const EvaluationResult: React.FC<{
  evaluationId: string
  evaluationStatus?: EvaluationStepStatus
  teacherAuthority?: boolean
}> = ({ evaluationId, evaluationStatus, teacherAuthority }) => {
  const result = useGetDetailedEvaluatonQuery({ variables: { evaluationId } })
  const gradeData = useGrade(evaluationId, evaluationStatus)

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { evaluation } = result.data

  // On Init
  let gradeView = null
  if (gradeData.grade !== null && gradeData.grade !== undefined) {
    gradeView = (
      <p className="grade-view-container">
        <GradeView grade={gradeData.grade} />
      </p>
    )
  }

  return (
    <>
      {gradeView ? gradeView : null}
      {evaluation.steps.map(step => (
        <EvaluationStepPanel
          answerId={evaluation.answer.id}
          stepBasics={step}
          key={step.id}
          fetchGrade={gradeData.fetchGrade}
          teacherAuthority={!!teacherAuthority}
        />
      ))}
    </>
  )
}

export default EvaluationResult
