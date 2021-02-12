import { Col, Icon, Row, Table, Tooltip } from 'antd'
import {
  GetScoreboardByAssignmentIdQuery
} from '../../generated/graphql'
import '../SubmissionsTable.less'
import EditNickname from "./EditNickname";
import React from "react";

type AssignmentScoreboard = NonNullable<
  GetScoreboardByAssignmentIdQuery['scoreboardByAssignmentId']
  >//['']

type SubmissionsScoreboard = AssignmentScoreboard['submissionsScoreboard'][number]
type AnswersScoreboard = SubmissionsScoreboard['answersScoreboard'][number]
type TaskScoreboard = AssignmentScoreboard['tasksScoreboard'][number]
type UserAlias = SubmissionsScoreboard['useralias']
type GradeScoreboard = AnswersScoreboard['gradeScoreboard']

const { Column } = Table

const alphabeticSorter = (
  extractProperty: (x: SubmissionsScoreboard) => string | null | undefined
) => (a: SubmissionsScoreboard, b: SubmissionsScoreboard) => {
  const valA = extractProperty(a) || ''
  const valB = extractProperty(b) || ''
  return valA.localeCompare(valB)
}


const ScoreboardTable: React.FC<{
  scoreboardByAssignmentId: AssignmentScoreboard
  fetchScoreboard: any
}> =props=> {

  const assignments = props.scoreboardByAssignmentId
  const allSubmissions = assignments.submissionsScoreboard


  const titleFunc = () => {
    return (
      <Row gutter={16}>
        <Col span={6}>
          <EditNickname editable={true} onChange={props.fetchScoreboard}/>
        </Col>

      </Row>
    )
  }

  // 700px = width of first columns
  // 200px = min width for each task column
  const scrollX = assignments.submissionsScoreboard.length * 200

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
        dataIndex="useralias.alias"
        width={200}
        fixed="left"
        defaultSortOrder="ascend"
        sorter={alphabeticSorter(submission => submission.useralias.alias)}
      />

      {taskColumnRenderer( assignments.tasksScoreboard)}
    </Table>
  )
}

const getAnswerFromSubmission = (
  submission: SubmissionsScoreboard,
  taskScoreboard: TaskScoreboard
): AnswersScoreboard | undefined =>
  submission.answersScoreboard.find(candidate => candidate.taskScoreboard.id === taskScoreboard.id)

const taskColumnRenderer = (
  tasks: TaskScoreboard[]
) => {
  const renderAnswer = (task: TaskScoreboard, submission: SubmissionsScoreboard) => {
    //There should always be a grade defined
    if(getAnswerFromSubmission(submission,task)!==undefined){
      const grade = getAnswerFromSubmission(submission, task)!!.gradeScoreboard

      if (grade==null || !grade.calculated) {
        return (
          <Tooltip title="No Grade Calculated">
            <Icon type="stop" className="no-answer" />
          </Tooltip>
        )
      }else{
        return (
          <div>{(Math.round(grade.gradePercentage * 100) / 100).toFixed(2)}%</div>
        )
      }
    }
  }

  // column width is determined by scrollX of the table
  return tasks.map(task => {
    return (
      <Column
        key={`task-${task.id}`}
        title={task.title}
        align="center"
        render={renderAnswer.bind(undefined, task)}
      />
    )
  })
}

export default ScoreboardTable
