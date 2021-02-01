import React, { useEffect, useState } from 'react'
import {
  EvaluationStepBasicsFragment,
  EvaluationStepResult,
  EvaluationStepStatus,
  Feedback,
  FeedbackSeverity,
  FeedbackStatus,
  useEvaluationStepStatusUpdatedSubscription,
  useGetEvaluationStepQuery
} from '../generated/graphql'
import { SmileTwoTone } from '@ant-design/icons'
import { Card, Collapse, Empty, Result, Skeleton, Tag } from 'antd'
import SortSelect from './SortSelect'
import SyntaxHighlighter from './code/SyntaxHighlighter'
import { compare } from '../services/util'
import renderFeedbackPanel from './FeedbackPanel'
import EvaluationStepIcon from './EvaluationStepIcon'
import { isEvaluationInProgress } from '../services/evaluation'
import EvaluationProcessingIcon from './EvaluationProcessingIcon'
import Countdown from './Countdown'
import moment from 'moment'
import { momentDifferenceToRelTime } from '../services/time'

function timer(queuedAt?: string, finishedAt?: string) {
  if (queuedAt && !finishedAt) {
    return (
      <Tag>
        {"Running since "}
        <Countdown date={moment(queuedAt)} overtime />
      </Tag>
    )
  }
  if (queuedAt && finishedAt) {
    return (
      <Tag>
        {"Duration "}
        {momentDifferenceToRelTime(moment(finishedAt), moment(queuedAt))}
      </Tag>
    )
  }
}

export const EvaluationStepPanel: React.FC<{
  answerId: string
  stepBasics: EvaluationStepBasicsFragment
}> = props => {
  const { answerId, stepBasics } = props
  const { id: stepId } = stepBasics

  const [stepStatus, setStepStatus] = useState<EvaluationStepStatus>(
    stepBasics.status
  )
  const [stepResult, setStepResult] = useState<
    EvaluationStepResult | undefined
  >(stepBasics.result || undefined)
  const [sortValue, setSortValue] = useState('FILE')
  const result = useGetEvaluationStepQuery({ variables: { stepId } })
  const { refetch } = result
  const { data: liveData } = useEvaluationStepStatusUpdatedSubscription({
    variables: { stepId }
  })

  useEffect(() => {
    const updatedStep = liveData?.evaluationStepStatusUpdated
    if (updatedStep?.status) {
      setStepStatus(updatedStep?.status)
    }
    if (updatedStep?.result) {
      setStepResult(updatedStep.result)
    }
  }, [liveData, setStepStatus, setStepResult])

  useEffect(() => {
    const updatedStep = liveData?.evaluationStepStatusUpdated
    if (updatedStep?.status === EvaluationStepStatus.Finished) {
      refetch()
    }
  }, [refetch, liveData])

  const title = (
    <>
      <EvaluationStepIcon status={stepStatus} result={stepResult} />{' '}
      {stepBasics.definition.title + ' '}
      {stepBasics.definition.runner.stoppable &&
        timer(
          stepBasics.queuedAt || undefined,
          stepBasics.finishedAt || undefined
        )}
    </>
  )

  if (result.loading || !result.data) {
    return (
      <Card title={title} className="evaluation-result-panel">
        <Skeleton />
      </Card>
    )
  }
  const { evaluationStep: step } = result.data

  const feedbackList = step.feedback
  const handleSortChange = (value: string) => setSortValue(value)
  const FeedbackSortOptions: string[] = Object.keys(FeedbackSortMethods)
  const sorter = (
    <SortSelect
      defaultValue={sortValue}
      values={FeedbackSortOptions}
      onSortChange={handleSortChange}
    />
  )

  let body
  if (!step.feedback || step.feedback.length === 0) {
    if (step.result === EvaluationStepResult.Success) {
      body = (
        <Result icon={<SmileTwoTone />} title="All checks passed – good job!" />
      )
    } else if (step.summary) {
      body = <SyntaxHighlighter>{step.summary}</SyntaxHighlighter>
    }
  } else {
    const renderFeedback = (feedback: Feedback) =>
      renderFeedbackPanel({
        feedback,
        answerId
      })
    const renderedFeedbackList = feedbackList
      .slice()
      .sort(FeedbackSortMethods[sortValue])
      .map(renderFeedback)

    body = <Collapse>{renderedFeedbackList}</Collapse>
  }

  if (!body && isEvaluationInProgress(stepStatus)) {
    body = (
      <Result
        icon={<EvaluationProcessingIcon />}
        title={`"${stepBasics.definition.title}" is in progress…`}
      />
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

const FeedbackSortMethods: Record<
  string,
  (a: Feedback, b: Feedback) => number
> = {
  SEVERITY: (a, b) =>
    compare(a.severity, b.severity, value =>
      value ? severityOrder[value] : 0
    ),
  STATUS: (a, b) =>
    compare(a.status, b.status, value => (value ? statusOrder[value] : 0)),
  FILE: (a, b) => {
    if (a.fileContext && b.fileContext) {
      if (a.fileContext.path === b.fileContext.path) {
        return (a.fileContext.lineStart || 0) - (b.fileContext.lineStart || 0)
      }
      return a.fileContext.path.localeCompare(b.fileContext.path)
    } else if (a.fileContext) {
      return -1
    } else if (b.fileContext) {
      return 1
    }
    return 0
  }
}

export default EvaluationStepPanel
