import React from 'react'
import { useGetDetailedEvaluatonQuery } from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'
import './EvaluationResult.less'
import EvaluationStepPanel from './EvaluationStepPanel'

const EvaluationResult: React.FC<{ evaluationId: string }> = ({
  evaluationId
}) => {
  const result = useGetDetailedEvaluatonQuery({ variables: { evaluationId } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { evaluation } = result.data
  return (
    <>
      {evaluation.steps.map(step => (
        <EvaluationStepPanel
          answerId={evaluation.answer.id}
          stepBasics={step}
          key={step.id}
        />
      ))}
    </>
  )
}

export default EvaluationResult
