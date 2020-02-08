import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Card } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import EntityLink from '../../components/EntityLink'
import {
  GetAssignmentListQueryResult,
  useGetAssignmentListQuery
} from '../../services/codefreak-api'

const AssignmentListPage: React.FC = () => {
  const result = useGetAssignmentListQuery()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignments } = result.data
  return (
    <>
      <PageHeaderWrapper
        extra={
          <Authorized authority="ROLE_TEACHER">
            <Link to="/assignments/create" key="1">
              <Button type="primary" icon="plus">
                Create Assignment
              </Button>
            </Link>
          </Authorized>
        }
      />
      {assignments.map(renderAssignment)}
    </>
  )
}

const renderAssignment = (
  assignment: NonNullable<
    GetAssignmentListQueryResult['data']
  >['assignments'][number]
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
      <EntityLink to={assignment}>
        <Button icon="folder-open" type="primary">
          Details
        </Button>
      </EntityLink>
      <Authorized authority="ROLE_TEACHER">
        {' '}
        <EntityLink to={assignment} sub="/submissions">
          <Button icon="table">Student Submissions</Button>
        </EntityLink>
      </Authorized>
    </Card>
  )
}

export default AssignmentListPage
