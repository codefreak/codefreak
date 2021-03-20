import { Card, Collapse, Empty, Icon, Result, Typography } from 'antd'
import React, { useState } from 'react'
import ReactMarkdown from 'react-markdown'
import {
  EvaluationStep,
  EvaluationStepResult,
  EvaluationStepStatus,
  Feedback,
  Feedback as FeedbackEntity,
  FeedbackSeverity,
  FeedbackStatus,
  useGetDetailedEvaluatonQuery
} from '../generated/graphql'
import React from 'react'
import { useGetDetailedEvaluatonQuery } from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'
import './EvaluationResult.less'
import EvaluationStepPanel from './EvaluationStepPanel'

const EvaluationResult: React.FC<{
  evaluationId: string
  evaluationStepStatus?: EvaluationStepStatus
}> = ({ evaluationId, evaluationStepStatus }) => {
  const result = useGetDetailedEvaluatonQuery({ variables: { evaluationId } })
  const gradeData = useGetGrade(evaluationId, evaluationStepStatus)

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { evaluation } = result.data

  // On Init
  let gradeView
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
