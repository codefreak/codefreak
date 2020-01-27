import { Card, Collapse, Typography } from 'antd'
import React from 'react'
import {
  EvaluationStep,
  Feedback,
  Feedback as FeedbackEntity,
  useGetDetailedEvaluatonQuery
} from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'
import EvaluationStepResultIcon from './EvaluationStepResultIcon'

const { Text } = Typography

const FileContext: React.FC<{ data: FeedbackEntity['fileContext'] }> = ({
  data
}) => {
  if (!data) {
    return null
  }
  let text = data.path

  if (data.lineStart) {
    text += `:${data.lineStart}`
  }
  if (data.lineEnd) {
    text += `-${data.lineEnd}`
  }

  return (
    <Text style={{ float: 'right' }} code>
      {text}
    </Text>
  )
}

const renderFeedbackPanel = (feedback: Feedback) => {
  const title = (
    <>
      {feedback.summary} <FileContext data={feedback.fileContext} />
    </>
  )
  return (
    <Collapse.Panel header={title} key={feedback.id}>
      <pre>
        <code>{feedback.longDescription}</code>
      </pre>
    </Collapse.Panel>
  )
}

const EvaluationResult: React.FC<{ evaluationId: string }> = ({
  evaluationId
}) => {
  const result = useGetDetailedEvaluatonQuery({ variables: { evaluationId } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { evaluation } = result.data
  return <>{evaluation.steps.map(renderEvaluationStep)}</>
}

const renderEvaluationStep = (step: EvaluationStep) => {
  const title = (
    <>
      <EvaluationStepResultIcon stepResult={step} /> {step.runnerName}
    </>
  )
  return (
    <Card title={title} style={{ marginBottom: 30 }} key={step.id}>
      <Collapse>{step.feedback.map(renderFeedbackPanel)}</Collapse>
    </Card>
  )
}

export default EvaluationResult
