import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Card } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import {
  GetAssignmentListProps,
  withGetAssignmentList
} from '../../services/codefreak-api'

const AssignmentListPage: React.FC<GetAssignmentListProps> = props => {
  const { assignments = [] } = props.data
  return (
    <AsyncPlaceholder result={props.data}>
      <PageHeaderWrapper
        extra={
          <Authorized role="TEACHER">
            <Link to="/assignments/create" key="1">
              <Button type="primary" icon="plus">
                Create
              </Button>
            </Link>
          </Authorized>
        }
      />
      {assignments.map(renderAssignment)}
    </AsyncPlaceholder>
  )
}

const renderAssignment = (
  assignment: NonNullable<GetAssignmentListProps['data']['assignments']>[number]
) => {
  return (
    <Card
      title={assignment.title}
      key={assignment.id}
      style={{ marginBottom: 16 }}
    >
      <p>
        {assignment.tasks.length}{' '}
        {assignment.tasks.length === 1 ? 'task' : 'tasks'}
      </p>
      <Link to={`/assignments/${assignment.id}`}>
        <Button icon="folder-open" type="primary">
          Details
        </Button>
      </Link>
      <Authorized role="TEACHER">
        {' '}
        <Button icon="table">Student Submissions</Button>
      </Authorized>
    </Card>
  )
}

export default withGetAssignmentList()(AssignmentListPage)
