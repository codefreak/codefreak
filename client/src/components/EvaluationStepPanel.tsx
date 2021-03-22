import React, { useState } from 'react'
import {
  EvaluationStepBasicsFragment,
  EvaluationStepResult,
  Feedback,
  FeedbackSeverity,
  FeedbackStatus
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
import useLiveEvaluationStep from '../hooks/useLiveEvaluationStep'
import Countdown from './Countdown'
import moment from 'moment'
import { momentDifferenceToRelTime } from '../services/time'
import PointsEdit from './autograder/PointsEdit'

function timer(queuedAt?: string, finishedAt?: string) {
  if (queuedAt && !finishedAt) {
    return (
      <Tag>
        {'Running since '}
        <Countdown date={moment(queuedAt)} overtime />
      </Tag>
    )
  }
  if (queuedAt && finishedAt) {
    return (
      <Tag>
        {'Duration '}
        {momentDifferenceToRelTime(moment(finishedAt), moment(queuedAt))}
      </Tag>
    )
  }
}

interface EvaluationStepPanelProps {
  answerId: string
  stepBasics: EvaluationStepBasicsFragment
  fetchGrade: any
}

export const EvaluationStepPanel: React.FC<EvaluationStepPanelProps> = props => {
  const { answerId, stepBasics } = props
  const [sortValue, setSortValue] = useState('FILE')
  const stepDetailsQuery = useLiveEvaluationStep(stepBasics.id)

  const stepDetails = stepDetailsQuery.data?.evaluationStep
  const stepStatus = stepDetails?.status || stepBasics.status
  const stepResult = stepDetails?.result || stepBasics.result || undefined
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

  if (stepDetailsQuery.loading || !stepDetailsQuery.data) {
    return (
      <Card title={title} className="evaluation-result-panel">
        <Skeleton />
      </Card>
    )
  }
  const { evaluationStep: step } = stepDetailsQuery.data

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
        <div>
          <Result
            icon={<SmileTwoTone />}
            title="All checks passed – good job!"
          />
          <PointsEdit
            evaluationStepId={step.id}
            fetchGrade={props.fetchGrade}
          />
        </div>
      )
    } else if (step.summary) {
      body = (
        <div>
          <SyntaxHighlighter>{step.summary}</SyntaxHighlighter>{' '}
          <PointsEdit
            evaluationStepId={step.id}
            fetchGrade={props.fetchGrade}
          />
        </div>
      )
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

    body = (
      <div>
        <Collapse>{renderedFeedbackList}</Collapse>{' '}
        <PointsEdit evaluationStepId={step.id} fetchGrade={props.fetchGrade} />
      </div>
    )
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
