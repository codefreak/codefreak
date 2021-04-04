import { Col, Row, Table, Tooltip } from 'antd'
import { GetScoreboardByAssignmentIdQuery } from '../../generated/graphql'
import '../SubmissionsTable.less'
import EditNickname from './EditNickname'
import React from 'react'
import Icon from '@ant-design/icons'

type AssignmentScoreboard = NonNullable<
  GetScoreboardByAssignmentIdQuery['scoreboardByAssignmentId']
>
type SubmissionsScoreboard = AssignmentScoreboard['submissionsScoreboard'][number]
type AnswersScoreboard = SubmissionsScoreboard['answersScoreboard'][number]
type TaskScoreboard = AnswersScoreboard['taskScoreboard']

const { Column } = Table

const defaultSort = (a: number, b: number) => {
  if (a < b) return -1
  if (b < a) return 1
  return 0
}
export const Sorter = {
  DEFAULT: defaultSort
}

const getAnswerFromSubmission = (
  submission: SubmissionsScoreboard,
  taskScoreboard: TaskScoreboard
): AnswersScoreboard | undefined =>
  submission.answersScoreboard.find(
    candidate => candidate.taskScoreboard.id === taskScoreboard.id
  )

const alphabeticSorter = (
  extractProperty: (x: SubmissionsScoreboard) => string | null | undefined
) => (a: SubmissionsScoreboard, b: SubmissionsScoreboard) => {
  const valA = extractProperty(a) || ''
  const valB = extractProperty(b) || ''
  return valA.localeCompare(valB)
}

const numericSorter = (
  a: SubmissionsScoreboard,
  b: SubmissionsScoreboard,
  task: TaskScoreboard
) => {
  const valA =
    getAnswerFromSubmission(a, task)?.gradeScoreboard?.gradePercentage || 0
  const valB =
    getAnswerFromSubmission(b, task)?.gradeScoreboard?.gradePercentage || 0
  return Sorter.DEFAULT(valA, valB)
}

const ScoreboardTable: React.FC<{
  scoreboardByAssignmentId: AssignmentScoreboard
  fetchScoreboard: any
}> = props => {
  const assignments = props.scoreboardByAssignmentId
  const allSubmissions = assignments.submissionsScoreboard

  const titleFunc = () => {
    return (
      <Row gutter={16}>
        <Col span={6}>
          <EditNickname editable onChange={props.fetchScoreboard} />
        </Col>
      </Row>
    )
  }

  // 700px = width of first columns
  // 200px = min width for each task column
  const scrollX = assignments.submissionsScoreboard.length * 100

  return (
    <Table
      dataSource={allSubmissions}
      pagination={{
        pageSize: 100,
        hideOnSinglePage: true
      }}
      bordered
      className="submissions-table"
      rowKey="id"
      title={titleFunc}
      scroll={{
        x: scrollX
      }}
    >
      <Column
        title="Nickname"
        dataIndex={['useralias', 'alias']}
        width={200}
        fixed="left"
        sortDirections={['ascend', 'descend', 'ascend']}
        sorter={alphabeticSorter(submission => submission.useralias.alias)}
      />
      {taskColumnRenderer(assignments.tasksScoreboard)}
    </Table>
  )
}

const taskColumnRenderer = (tasks: TaskScoreboard[]) => {
  const renderAnswer = (
    task: TaskScoreboard,
    submission: SubmissionsScoreboard
  ) => {
    // There should always be a grade defined
    if (getAnswerFromSubmission(submission, task) !== undefined) {
      const answer = getAnswerFromSubmission(submission, task)
      // answer.
      if (
        answer?.gradeScoreboard === null ||
        !answer?.gradeScoreboard?.calculated
      ) {
        return (
          <Tooltip title="No Grade Calculated">
            <Icon type="stop" className="no-answer" />
          </Tooltip>
        )
      } else {
        return (
          <div>
            {(
              Math.round(answer.gradeScoreboard.gradePercentage * 100) / 100
            ).toFixed(2)}
            %
          </div>
        )
      }
    } else {
      return null
    }
  }

  // column width is determined by scrollX of the table
  return tasks.map(task => {
    const jumpRender = (a: SubmissionsScoreboard, b: SubmissionsScoreboard) => {
      return numericSorter(a, b, task)
    }

    return (
      <Column
        key={`task-${task.id}`}
        title={task.title}
        align="center"
        render={renderAnswer.bind((submission: any) => submission, task)}
        sortDirections={['ascend', 'descend', 'ascend']}
        sorter={jumpRender}
      />
    )
  })
}

export default ScoreboardTable