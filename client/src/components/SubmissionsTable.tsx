import { Button, Col, Icon, Input, Row, Table, Tooltip } from 'antd'
import React, { ChangeEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  EvaluationStepResult,
  GetAssignmentWithSubmissionsQueryResult,
  PendingEvaluationStatus
} from '../generated/graphql'
import useAnswerEvaluation from '../hooks/useAnswerEvaluation'
import { getEntityPath } from '../services/entity-path'
import { shorten } from '../services/short-id'
import { matches } from '../services/strings'
import ArchiveDownload from './ArchiveDownload'
import EvaluationResultPopover from './EvaluationResultPopover'
import EvaluationStepResultIcon from './EvaluationStepResultIcon'
import './SubmissionsTable.less'

type Assignment = NonNullable<
  GetAssignmentWithSubmissionsQueryResult['data']
>['assignment']
type Submission = Assignment['submissions'][number]
type Answer = Submission['answers'][number]
type Task = Assignment['tasks'][number]
type User = Submission['user']

const { Column } = Table

const alphabeticSorter = (
  extractProperty: (x: Submission) => string | null | undefined
) => (a: Submission, b: Submission) => {
  const valA = extractProperty(a) || ''
  const valB = extractProperty(b) || ''
  return valA.localeCompare(valB)
}

const SEARCHABLE_USER_COLUMNS: (keyof User)[] = [
  'username',
  'firstName',
  'lastName'
]

const searchSubmissions = (submissions: Submission[], criteria: string) => {
  return submissions.filter(submission => {
    return (
      SEARCHABLE_USER_COLUMNS.find(column =>
        matches(criteria, submission.user[column] || '')
      ) !== undefined
    )
  })
}

const SubmissionsTable: React.FC<{ assignment: Assignment }> = ({
  assignment
}) => {
  const allSubmissions = assignment.submissions
  const [searchCriteria, setSearchCriteria] = useState<string>()

  const submissions = searchCriteria?.trim().length
    ? searchSubmissions(allSubmissions, searchCriteria.trim())
    : allSubmissions

  const submissionSearch = (e: ChangeEvent<HTMLInputElement>) => {
    setSearchCriteria(e.target.value)
  }

  const titleFunc = () => {
    return (
      <Row>
        <Col span={6}>
          <Input.Search
            addonBefore="Search User"
            placeholder="first name, last name or username"
            allowClear
            onChange={submissionSearch}
          />
        </Col>
        <Col span={18} style={{ textAlign: 'right' }}>
          <Button
            type="default"
            href={`${assignment.submissionsDownloadUrl}.csv`}
            icon="table"
          >
            Download results as .csv
          </Button>
          <ArchiveDownload url={assignment.submissionsDownloadUrl}>
            Download files of all submissions…
          </ArchiveDownload>
        </Col>
      </Row>
    )
  }

  const footerFunc = () => {
    let text = `${allSubmissions.length} Submissions`
    if (allSubmissions.length > submissions.length) {
      text = `Showing ${submissions.length} of ` + text
    }
    return <div style={{ textAlign: 'right' }}>{text}</div>
  }

  return (
    <Table
      dataSource={submissions}
      bordered
      className="submissions-table"
      rowKey="id"
      title={titleFunc}
      footer={footerFunc}
    >
      <Column
        title="Last Name"
        dataIndex="user.lastName"
        width="10%"
        defaultSortOrder="ascend"
        sorter={alphabeticSorter(submission => submission.user.lastName)}
      />
      <Column
        title="First Name"
        dataIndex="user.firstName"
        width="10%"
        sorter={alphabeticSorter(submission => submission.user.firstName)}
      />
      <Column
        title="Username"
        dataIndex="user.username"
        width="20%"
        sorter={alphabeticSorter(submission => submission.user.username)}
      />
      {renderTaskColumnGroups(assignment.tasks)}
    </Table>
  )
}

const AnswerSummary: React.FC<{
  task: Task
  user: Submission['user']
  answer: Answer
}> = ({ task, user, answer }) => {
  const {
    latestEvaluation,
    pendingEvaluationStatus,
    loading
  } = useAnswerEvaluation(
    answer.id,
    answer.latestEvaluation,
    answer.pendingEvaluation?.status
  )

  // prevent flashing of old evaluation result by also showing loading indicator for fetching new results
  if (
    loading ||
    pendingEvaluationStatus === PendingEvaluationStatus.Queued ||
    pendingEvaluationStatus === PendingEvaluationStatus.Running
  ) {
    return (
      <Tooltip title="Evaluating answer…">
        <Icon type="loading" />
      </Tooltip>
    )
  }

  return (
    <EvaluationStepOverview
      task={task}
      user={user}
      evaluation={latestEvaluation}
    />
  )
}

const EvaluationStepOverview: React.FC<{
  task: Task
  user: Submission['user']
  evaluation: Answer['latestEvaluation']
}> = ({ evaluation, task, user }) => {
  const detailsLink = (
    <Link to={getEntityPath(task) + '/evaluation?user=' + shorten(user.id)}>
      <Button type="primary">
        <Icon type="bars" /> Details
      </Button>
    </Link>
  )

  if (!evaluation) {
    return (
      <div className="evaluation-step-results">
        <Icon type="question-circle" />
        <Tooltip title="Answer has not been evaluated, yet">
          {detailsLink}
        </Tooltip>
      </div>
    )
  }

  return (
    <>
      <div className="evaluation-step-results">
        {evaluation.steps.map(step => (
          <EvaluationStepResultIcon key={step.id} stepResult={step.result} />
        ))}
        <EvaluationResultPopover
          task={task}
          user={user}
          steps={evaluation.steps}
        >
          {detailsLink}
        </EvaluationResultPopover>
      </div>
    </>
  )
}

// TODO: Multiple filters are not working. filterValues is always a single value
const buildEvaluationFilter = (task: Task) => (
  filterValue: string,
  submission: Submission
) => {
  const answer = getAnswerFromSubmission(submission, task)
  const latestEvaluation = answer ? answer.latestEvaluation : null
  switch (filterValue) {
    case 'successful':
      return (
        !!latestEvaluation &&
        latestEvaluation.stepsResultSummary === EvaluationStepResult.Success
      )
    case 'failed':
      return (
        !!latestEvaluation &&
        latestEvaluation.stepsResultSummary === EvaluationStepResult.Failed
      )
    case 'no-answer':
    default:
      return !answer || !latestEvaluation
  }
}

const getAnswerFromSubmission = (
  submission: Submission,
  task: Task
): Answer | undefined =>
  submission.answers.find(candidate => candidate.task.id === task.id)

const renderTaskColumnGroups = (tasks: Task[]) => {
  const renderAnswer = (task: Task, submission: Submission) => {
    const answer = getAnswerFromSubmission(submission, task)

    if (!answer) {
      return (
        <Tooltip title="No answer submitted">
          <Icon type="stop" className="no-answer" />
        </Tooltip>
      )
    }

    return <AnswerSummary user={submission.user} task={task} answer={answer} />
  }

  // distribute remaining 60% width over all task columns
  const width = Math.floor(60 / tasks.length)
  return tasks.map(task => {
    return (
      <Column
        key={`task-${task.id}`}
        width={`${width}%`}
        title={task.title}
        align="center"
        filters={[
          { text: 'Successful', value: 'successful' },
          { text: 'Failed', value: 'failed' },
          { text: 'No answer', value: 'no-answer' }
        ]}
        onFilter={buildEvaluationFilter(task)}
        render={renderAnswer.bind(undefined, task)}
      />
    )
  })
}

export default SubmissionsTable
