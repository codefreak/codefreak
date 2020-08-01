import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Card, Descriptions, Modal, Tooltip } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import AssignmentStatusTag from '../../components/AssignmentStatusTag'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import EntityLink from '../../components/EntityLink'
import {
  GetAssignmentListQueryResult,
  useDeleteAssignmentMutation,
  useGetAssignmentListQuery
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'

const { confirm } = Modal

const AssignmentListPage: React.FC = () => {
  const result = useGetAssignmentListQuery()
  const [deleteAssignment] = useDeleteAssignmentMutation()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignments } = result.data

  const renderProps: RenderProps = {
    delete: async (id: string) => {
      const deleteResult = await deleteAssignment({ variables: { id } })
      if (deleteResult.data) {
        messageService.success('Assignment removed')
        result.refetch()
      }
    }
  }

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
      {assignments.map(renderAssignment(renderProps))}
    </>
  )
}

type Assignment = NonNullable<
  GetAssignmentListQueryResult['data']
>['assignments'][number]

interface RenderProps {
  delete: (assignmentId: string) => Promise<any>
}

const renderAssignment = (props: RenderProps) => (assignment: Assignment) => {
  const confirmDelete = () =>
    confirm({
      title: 'Are you sure?',
      content:
        'You are deleting an assignment. The original task templates will stay in the task pool. All other data (including student submissions) will be lost.',
      async onOk() {
        await props.delete(assignment.id)
      }
    })
  return (
    <Card
      title={
        <>
          {assignment.title} <AssignmentStatusTag status={assignment.status} />
        </>
      }
      key={assignment.id}
      style={{ marginBottom: 16 }}
      extra={
        assignment.deletable ? (
          <Tooltip title={'Delete assignment'} placement="left">
            <Button
              onClick={confirmDelete}
              type="dashed"
              shape="circle"
              icon="delete"
            />
          </Tooltip>
        ) : null
      }
    >
      <Descriptions>
        <Descriptions.Item label="Tasks">
          {assignment.tasks.length}
        </Descriptions.Item>
      </Descriptions>
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
