import { Icon, Table } from 'antd'
import React from 'react'
import ArchiveDownload from '../../components/ArchiveDownload'
import AsyncPlaceholder from '../../components/AsyncContainer'
import {
  GetAssignmentQueryResult,
  GetSubmissionListQueryResult,
  useGetAssignmentQuery,
  useGetSubmissionListQuery
} from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'

const { Column, ColumnGroup } = Table

type Submission = NonNullable<
  GetSubmissionListQueryResult['data']
>['submissions'][number]
type Answer = Submission['answers'][number]
type Assignment = NonNullable<GetAssignmentQueryResult['data']>['assignment']
type Task = Assignment['tasks'][number]
type EvaluationStepDefinition = Task['evaluationSteps'][number]

const SubmissionListPage: React.FC = () => {
  const assignmentId = useIdParam()
  const submissionsResult = useGetSubmissionListQuery({
    variables: { assignmentId }
  })
  const assignmentResult = useGetAssignmentQuery({
    variables: { id: assignmentId }
  })

  if (submissionsResult.data === undefined) {
    return <AsyncPlaceholder result={submissionsResult} />
  }
  if (assignmentResult.data === undefined) {
    return <AsyncPlaceholder result={assignmentResult} />
  }

  const { submissions } = submissionsResult.data
  const { assignment } = assignmentResult.data

  return (
    <Table dataSource={submissions} bordered>
      <Column title="First Name" key="firstName" dataIndex="user.firstName" />
      <Column title="Last Name" key="lastName" dataIndex="user.lastName" />
      <Column title="Username" key="email" dataIndex="user.username" />
      {renderTaskColumnGroups(assignment.tasks)}
    </Table>
  )
}

const renderEvalResultColumn = (
  task: Task,
  step: EvaluationStepDefinition,
  submission: Submission
) => {
  return (
    <>
      TODO: Data
    </>
  )
}

const renderTaskColumnGroups = (tasks: Task[]) => {
  const renderEvaluationColumn = (
    task: Task,
    step: EvaluationStepDefinition
  ) => {
    return (
      <Column
        title={step.runnerName}
        key={`eval-step-${step.index}`}
        render={renderEvalResultColumn.bind(undefined, task, step)}
      />
    )
  }

  const renderSubmissionDownload = (submission: Submission, task: Task) => {
    const answer = submission.answers.find(
      candidate => candidate.task.id === task.id
    )

    if (answer === undefined) {
      return <Icon type="stop" title="No answer"/>
    }

    return <ArchiveDownload url={answer.sourceUrl} />
  }

  return tasks.map(task => {
    return (
      <ColumnGroup key={`download-${task.id}`} title={task.title}>
        <Column
          align="center"
          render={submission => renderSubmissionDownload(submission, task)}
        />
        {task.evaluationSteps.map(renderEvaluationColumn.bind(undefined, task))}
      </ColumnGroup>
    )
  })
}

export default SubmissionListPage
