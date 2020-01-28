import { Card, Collapse, Empty, Icon, Result, Typography } from 'antd'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import {
  EvaluationStep,
  EvaluationStepResult,
  Feedback,
  Feedback as FeedbackEntity,
  FeedbackSeverity,
  FeedbackStatus,
  useGetDetailedEvaluatonQuery
} from '../generated/graphql'
import AsyncPlaceholder from './AsyncContainer'
import './EvaluationResult.less'
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

  return <Text code>{text}</Text>
}

const severityIconMap: Record<FeedbackSeverity, string> = {
  INFO: 'info-circle',
  MINOR: 'warning',
  MAJOR: 'exclamation-circle',
  CRITICAL: 'close-circle'
}
const FeedbackSeverityIcon: React.FC<{ severity: FeedbackSeverity }> = ({
  severity
}) => {
  let iconType = 'question-circle'
  if (severity && severityIconMap[severity]) {
    iconType = severityIconMap[severity]
  }
  const severityClass = severity ? severity.toString().toLowerCase() : 'default'
  return (
    <Icon
      type={iconType}
      className={`feedback-icon feedback-icon-severity-${severityClass}`}
    />
  )
}

const renderFeedbackPanel = (feedback: Feedback) => {
  let icon = null
  // either show the success icon or the severity of failure
  switch (feedback.status) {
    case FeedbackStatus.Failed:
      icon = feedback.severity ? (
        <FeedbackSeverityIcon severity={feedback.severity} />
      ) : null
      break
    case FeedbackStatus.Success:
      icon = (
        <Icon
          type="check-circle"
          className="feedback-icon feedback-icon-success"
        />
      )
      break
    case FeedbackStatus.Ignore:
      icon = (
        <Icon type="forward" className="feedback-icon feedback-icon-ignore" />
      )
      break
  }

  const title = (
    <>
      {icon}
      <ReactMarkdown
        source={feedback.summary}
        allowedTypes={[
          'inlineCode',
          'text',
          'strong',
          'delete',
          'emphasis',
          'link'
        ]}
        unwrapDisallowed
      />
    </>
  )
  let body = null
  if (feedback.longDescription) {
    body = <ReactMarkdown source={feedback.longDescription} />
  }

  return (
    <Collapse.Panel
      header={title}
      extra={<FileContext data={feedback.fileContext} />}
      key={feedback.id}
    >
      {body}
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
  let body = null
  if (!step.feedback || step.feedback.length === 0) {
    if (step.result === EvaluationStepResult.Success) {
      body = (
        <Result
          icon={<Icon type="smile" theme="twoTone" />}
          title="All checks passed – good job!"
        />
      )
    } else {
      //
      body = <Empty />
    }
  } else {
    body = <Collapse>{step.feedback.map(renderFeedbackPanel)}</Collapse>
  }
  return (
    <Card
      title={title}
      style={{ marginBottom: 30 }}
      key={step.id}
      className="evaluation-result-panel"
    >
      {body}
    </Card>
  )
}

export default EvaluationResult
