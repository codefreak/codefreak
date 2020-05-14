import { Card, Collapse, Empty, Icon, Result, Select, Typography } from 'antd'
import React, { useState } from 'react'
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
import SyntaxHighlighter from './code/SyntaxHighlighter'
import { CodeViewerCard } from './CodeViewer'
import './EvaluationResult.less'
import EvaluationStepResultIcon from './EvaluationStepResultIcon'

const { Text } = Typography

const FileReference: React.FC<{ data: FeedbackEntity['fileContext'] }> = ({
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

const LongDescriptionMarkdown: React.FC<{ source: string }> = ({ source }) => {
  return (
    <ReactMarkdown
      source={source}
      escapeHtml={false}
      className="feedback-long-description"
    />
  )
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

const renderFeedbackPanel = (answerId: string, feedback: Feedback) => {
  let icon = null
  // either show the success icon or the severity of failure
  switch (feedback.status) {
    case FeedbackStatus.Failed:
      if (feedback.severity) {
        icon = <FeedbackSeverityIcon severity={feedback.severity} />
      } else {
        icon = (
          <Icon
            type="exclamation-circle"
            className="feedback-icon feedback-icon-failed"
          />
        )
      }
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
  if (feedback.fileContext) {
    const { lineStart, lineEnd } = feedback.fileContext
    body = (
      <CodeViewerCard
        answerId={answerId}
        path={feedback.fileContext.path}
        lineStart={lineStart || undefined}
        lineEnd={lineEnd || undefined}
      />
    )
  }

  if (feedback.longDescription) {
    body = (
      <>
        {body}
        <LongDescriptionMarkdown source={feedback.longDescription} />
      </>
    )
  }

  return (
    <Collapse.Panel
      disabled={!body}
      showArrow={!!body}
      header={title}
      extra={<FileReference data={feedback.fileContext} />}
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
  return (
    <>
      {evaluation.steps.map(step => (
        <EvaluationStepPanel
          answerId={evaluation.answer.id}
          step={step}
          key={step.id}
        />
      ))}
    </>
  )
}

enum FeedbackSortOptions {
  SEVERITY,
  FILE,
  STATUS
}
const severityOrder: Record<FeedbackSeverity, number> = {
  INFO: 3,
  MINOR: 2,
  MAJOR: 1,
  CRITICAL: 0
}
const statusOrder: Record<FeedbackStatus, number> = {
  FAILED: 0,
  SUCCESS: 1,
  IGNORE: 2
}
const sortFeedbackList = (
  feedbackList: Feedback[],
  by: keyof typeof FeedbackSortOptions
) => {
  return feedbackList.sort((a: Feedback, b: Feedback) => {
    switch (by) {
      case 'STATUS':
        if (a.status && b.status) {
          return statusOrder[a.status] - statusOrder[b.status]
        } else if (a.status) {
          return -1
        } else if (b.status) {
          return 1
        }
        return 0
      case 'SEVERITY':
        if (a.severity && b.severity) {
          return severityOrder[a.severity] - severityOrder[b.severity]
        } else if (a.severity) {
          return -1
        } else if (b.severity) {
          return 1
        }
        return 0
      case 'FILE':
        if (a.fileContext && b.fileContext) {
          if (a.fileContext.path === b.fileContext.path) {
            return (
              (a.fileContext.lineStart || 0) - (b.fileContext.lineStart || 0)
            )
          }
          return a.fileContext.path.localeCompare(b.fileContext.path)
        } else if (a.fileContext) {
          return -1
        } else if (b.fileContext) {
          return 1
        }
        return 0
    }
    return 0
  })
}

const EvaluationStepPanel: React.FC<{
  answerId: string
  step: Omit<EvaluationStep, 'definition'> & {
    definition: Pick<EvaluationStep['definition'], 'title'>
  }
}> = ({ answerId, step }) => {
  const title = (
    <>
      <EvaluationStepResultIcon stepResult={step.result} />{' '}
      {step.definition.title}
    </>
  )

  const [sortValue, setSortValue] = useState<keyof typeof FeedbackSortOptions>(
    'FILE'
  )
  const feedbackList = sortFeedbackList(step.feedback, sortValue)
  const onSortChange = (value: keyof typeof FeedbackSortOptions) =>
    setSortValue(value)
  const sorter = (
    <Select
      defaultValue={sortValue}
      onChange={onSortChange}
      style={{ width: '150px' }}
    >
      <Select.Option value="STATUS">Sort by Status</Select.Option>
      <Select.Option value="FILE">Sort by File</Select.Option>
      <Select.Option value="SEVERITY">Sort by Severity</Select.Option>
    </Select>
  )

  let body
  if (!step.feedback || step.feedback.length === 0) {
    if (step.result === EvaluationStepResult.Success) {
      body = (
        <Result
          icon={<Icon type="smile" theme="twoTone" />}
          title="All checks passed – good job!"
        />
      )
    } else if (step.summary) {
      body = <SyntaxHighlighter>{step.summary}</SyntaxHighlighter>
    }
  } else {
    body = (
      <Collapse>
        {feedbackList.map(feedback => renderFeedbackPanel(answerId, feedback))}
      </Collapse>
    )
  }

  if (!body) {
    body = <Empty />
  }

  return (
    <Card
      title={title}
      key={step.id}
      extra={sorter}
      className="evaluation-result-panel"
    >
      {body}
    </Card>
  )
}

export default EvaluationResult
