import { Button, Col, Icon, Radio, Row, Table, Tooltip } from 'antd'
import { RadioGroupProps } from 'antd/es/radio'
import React, { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  EvaluationStepResult,
  GetAssignmentWithSubmissionsQueryResult,
  EvaluationStepStatus
} from '../generated/graphql'
import useAnswerEvaluation from '../hooks/useAnswerEvaluation'
import { useFormatter } from '../hooks/useFormatter'
import { getEntityPath } from '../services/entity-path'
import { shorten } from '../services/short-id'
import { matches } from '../services/strings'
import ArchiveDownload from './ArchiveDownload'
import EvaluationResultPopover from './EvaluationResultPopover'
import EvaluationStepResultIcon from './EvaluationStepResultIcon'
import './SubmissionsTable.less'
import SearchBar from './SearchBar'

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

type ColumnDisplay = 'eval-results' | 'answer-dates'

const SubmissionsTable: React.FC<{ assignment: Assignment }> = ({
  assignment
}) => {
  const { dateTime } = useFormatter()
  const allSubmissions = assignment.submissions
  const [searchCriteria, setSearchCriteria] = useState<string>()
  const [columnDisplay, setColumnDisplay] = useState<ColumnDisplay>(
    'eval-results'
  )

  const submissions = searchCriteria?.trim().length
    ? searchSubmissions(allSubmissions, searchCriteria.trim())
    : allSubmissions

  const handleSearch = (value: string) => {
    setSearchCriteria(value)
  }

  const onColumnDisplayChange: RadioGroupProps['onChange'] = useCallback(
    e => {
      setColumnDisplay(e.target.value)
    },
    [setColumnDisplay]
  )

  const titleFunc = () => {
    return (
      <Row gutter={16}>
        <Col span={6}>
          <SearchBar
            searchType="User"
            placeholder="by first-, last- or username"
            onChange={handleSearch}
          />
        </Col>
        <Col span={6}>
          <Radio.Group value={columnDisplay} onChange={onColumnDisplayChange}>
            <Radio.Button value="eval-results">
              Show Evaluation Results
            </Radio.Button>
            <Radio.Button value="answer-dates">
              Show Submission Dates
            </Radio.Button>
          </Radio.Group>
        </Col>
        <Col span={12} style={{ textAlign: 'right' }}>
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

  // 700px = width of first columns
  // 200px = min width for each task column
  const scrollX = 700 + assignment.tasks.length * 200

  return (
    <Table
      dataSource={submissions}
      pagination={{
        pageSize: 100,
        hideOnSinglePage: true
      }}
      bordered
      className="submissions-table"
      rowKey="id"
      title={titleFunc}
      footer={footerFunc}
      scroll={{
        x: scrollX
      }}
    >
      <Column
        title="Last Name"
        dataIndex="user.lastName"
        width={200}
        fixed="left"
        defaultSortOrder="ascend"
        sorter={alphabeticSorter(submission => submission.user.lastName)}
      />
      <Column
        title="First Name"
        dataIndex="user.firstName"
        width={200}
        fixed="left"
        sorter={alphabeticSorter(submission => submission.user.firstName)}
      />
      <Column
        title="Username"
        dataIndex="user.username"
        width={300}
        sorter={alphabeticSorter(submission => submission.user.username)}
      />
      {taskColumnRenderer(dateTime, columnDisplay, assignment.tasks)}
    </Table>
  )
}

const AnswerEvaluationSummary: React.FC<{
  task: Task
  user: Submission['user']
  answer: Answer
}> = ({ task, user, answer }) => {
  const {
    latestEvaluation,
    pendingEvaluationStatus,
    loading
  } = useAnswerEvaluation(answer.id, answer.latestEvaluation)

  // prevent flashing of old evaluation result by also showing loading indicator for fetching new results
  if (
    loading ||
    pendingEvaluationStatus === EvaluationStepStatus.Queued ||
    pendingEvaluationStatus === EvaluationStepStatus.Running
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

const taskColumnRenderer = (
  dateTimeFormat: (date: Date) => string,
  display: ColumnDisplay,
  tasks: Task[]
) => {
  const renderAnswer = (task: Task, submission: Submission) => {
    const answer = getAnswerFromSubmission(submission, task)

    if (!answer) {
      return (
        <Tooltip title="No answer submitted">
          <Icon type="stop" className="no-answer" />
        </Tooltip>
      )
    }
    if (display === 'answer-dates') {
      return dateTimeFormat(new Date(answer.updatedAt))
    } else {
      return (
        <AnswerEvaluationSummary
          user={submission.user}
          task={task}
          answer={answer}
        />
      )
    }
  }

  // column width is determined by scrollX of the table
  return tasks.map(task => {
    return (
      <Column
        key={`task-${task.id}`}
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
