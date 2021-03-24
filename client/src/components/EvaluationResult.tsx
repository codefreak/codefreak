import React from 'react'
import {
  EvaluationStepStatus,
  useGetDetailedEvaluatonQuery
} from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'
import './EvaluationResult.less'
import EvaluationStepPanel from './EvaluationStepPanel'
import useGetGrade from '../hooks/useGetGrade'
import GradeView from './autograder/GradeView'

const EvaluationResult: React.FC<{
  evaluationId: string
  evaluationStatus?: EvaluationStepStatus
}> = ({ evaluationId, evaluationStatus }) => {

  const result = useGetDetailedEvaluatonQuery({ variables: { evaluationId } })
  const gradeData = useGetGrade(evaluationId, evaluationStatus)

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { evaluation } = result.data

  // On Init
  let gradeView = null
  if (gradeData.grade !== undefined) {
    if (gradeData.grade !== null) {
      gradeView = (
        <p className="grade-view-container">
          <GradeView grade={gradeData.grade} />
        </p>
      )
    }
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
        />
      ))}
    </>
  )
}

export default EvaluationResult
