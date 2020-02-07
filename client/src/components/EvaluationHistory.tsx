import { Timeline } from 'antd'
import React from 'react'
import {
  EvaluationStepResult,
  GetEvaluationHistoryQueryResult,
  useGetEvaluationHistoryQuery
} from '../generated/graphql'
import usePendingEvaluation from '../hooks/usePendingEvaluation'
import AsyncPlaceholder from './AsyncContainer'
import { EvaluationErrorIcon } from './Icons'

const EvaluationHistory: React.FC<{ answerId: string }> = ({ answerId }) => {
  const result = useGetEvaluationHistoryQuery({ variables: { answerId } })

  const pendingEvaluation = usePendingEvaluation(answerId)

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const {
    answer: { evaluations }
  } = result.data

  return (
    <div style={{ padding: 32 }}>
      <Timeline reverse>{evaluations.map(renderEvaluation)}</Timeline>
    </div>
  )
}

const renderEvaluation = (
  evaluation: NonNullable<
    GetEvaluationHistoryQueryResult['data']
  >['answer']['evaluations'][0]
) => {
  return (
    <Timeline.Item
      key={evaluation.id}
      {...getDot(evaluation.stepsResultSummary)}
    >
      {new Date(evaluation.createdAt).toLocaleString()}
    </Timeline.Item>
  )
}

const getDot = (result: EvaluationStepResult) => {
  switch (result) {
    case 'SUCCESS':
      return { color: 'green' }
    case 'FAILED':
      return { color: 'red' }
    case 'ERRORED':
      return { dot: <EvaluationErrorIcon style={{ fontSize: '16px' }} /> }
  }
}

export default EvaluationHistory
