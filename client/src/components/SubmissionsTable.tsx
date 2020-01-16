import { Icon, Table } from 'antd'
import React from 'react'
import { GetAssignmentWithSubmissionsQueryResult } from '../generated/graphql'
import ArchiveDownload from './ArchiveDownload'
import './SubmissionsTable.less'

type Assignment = NonNullable<
  GetAssignmentWithSubmissionsQueryResult['data']
>['assignment']
type Submission = Assignment['submissions'][number]
type Answer = Submission['answers'][number]
type Task = Assignment['tasks'][number]

const { Column } = Table

const alphabeticSorter = (
  extractProperty: (x: Submission) => string | null | undefined
) => (a: Submission, b: Submission) => {
  const valA = extractProperty(a) || ''
  const valB = extractProperty(b) || ''
  return valA.localeCompare(valB)
}

const SubmissionsTable: React.FC<{ assignment: Assignment }> = ({
  assignment
}) => {
  return (
    <Table
      dataSource={assignment.submissions}
      bordered
      className="submissions-table"
      rowKey="id"
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

const getAnswerFromSubmission = (
  submission: Submission,
  task: Task
): Answer | undefined =>
  submission.answers.find(candidate => candidate.task.id === task.id)

const renderSubmissionDownload = (task: Task, submission: Submission) => {
  const answer = getAnswerFromSubmission(submission, task)

  if (answer === undefined) {
    return <Icon type="stop" title="No answer" />
  }

  return <ArchiveDownload url={answer.sourceUrl} />
}

const renderTaskColumnGroups = (tasks: Task[]) => {
  // distribute remaining 60% width over all task columns
  const width = Math.floor(60 / tasks.length)
  return tasks.map(task => {
    return (
      <Column
        key="download"
        width={`${width}%`}
        title={task.title}
        align="center"
        render={renderSubmissionDownload.bind(undefined, task)}
      />
    )
  })
}

export default SubmissionsTable
