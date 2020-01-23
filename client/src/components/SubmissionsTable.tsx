import {
  Button,
  Col,
  Dropdown,
  Icon,
  Input,
  Menu,
  Row,
  Table,
  Typography
} from 'antd'
import React, { ChangeEvent, useState } from 'react'
import {
  EvaluationStepResult,
  GetAssignmentWithSubmissionsQueryResult
} from '../generated/graphql'
import './SubmissionsTable.less'

type Assignment = NonNullable<
  GetAssignmentWithSubmissionsQueryResult['data']
>['assignment']
type Submission = Assignment['submissions'][number]
type Answer = Submission['answers'][number]
type Task = Assignment['tasks'][number]

const { Column } = Table
const { Text } = Typography

const alphabeticSorter = (
  extractProperty: (x: Submission) => string | null | undefined
) => (a: Submission, b: Submission) => {
  const valA = extractProperty(a) || ''
  const valB = extractProperty(b) || ''
  return valA.localeCompare(valB)
}

const filterSubmissions = (submissions: Submission[], criteria: string) => {
  const needle = criteria.toLocaleLowerCase()
  return submissions.filter(submission => {
    return submission.user.username.toLocaleLowerCase().indexOf(needle) !== -1
  })
}

const SubmissionsTable: React.FC<{ assignment: Assignment }> = ({
  assignment
}) => {
  const allSubmissions = assignment.submissions
  const [submissions, setSubmissions] = useState(allSubmissions)

  const submissionSearch = (e: ChangeEvent<HTMLInputElement>) => {
    setSubmissions(filterSubmissions(allSubmissions, e.target.value))
  }

  const titleFunc = () => {
    return (
      <Row>
        <Col span={6}>
          <Input.Search
            addonBefore="Search User"
            allowClear
            onChange={submissionSearch}
          />
        </Col>
        <Col span={18} style={{ textAlign: 'right' }}>
          <Button
            type="primary"
            href={assignment.submissionCsvUrl}
            icon="download"
          >
            Download results as .csv
          </Button>
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
      <Column render={renderActions(assignment)} />
    </Table>
  )
}

const renderActions = (assignment: Assignment) => (submission: Submission) => {
  const actionDropdown = (
    <Menu>
      {assignment.tasks.map(task => (
        <Menu.Item key={task.id}>{task.title}</Menu.Item>
      ))}
    </Menu>
  )
  return (
    <Dropdown overlay={actionDropdown} placement="bottomRight">
      <Button icon="more" title="Actions" />
    </Dropdown>
  )
}

const getAnswerFromSubmission = (
  submission: Submission,
  task: Task
): Answer | undefined =>
  submission.answers.find(candidate => candidate.task.id === task.id)

const renderAnswer = (task: Task, submission: Submission) => {
  const answer = getAnswerFromSubmission(submission, task)

  if (answer === undefined) {
    return <Icon type="stop" title="No answer" className="no-answer" />
  }

  if (!answer.latestEvaluation) {
    return <Icon type="question-circle" />
  }

  if (
    answer.latestEvaluation.stepsResultSummary === EvaluationStepResult.Success
  ) {
    return <Icon type="check-circle" />
  }

  // count the number of successful evaluation steps
  const successCount = answer.latestEvaluation.steps.reduce(
    (n, result) => (result.result === EvaluationStepResult.Success ? n + 1 : n),
    0
  )

  return (
    <Text type="warning">
      <Icon type="exclamation-circle" />
      <span className="evaluation-summary">
        {successCount}/{task.evaluationSteps.length}
      </span>
    </Text>
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

const renderTaskColumnGroups = (tasks: Task[]) => {
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
