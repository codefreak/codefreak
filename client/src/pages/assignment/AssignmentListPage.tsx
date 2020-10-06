import { PageHeaderWrapper } from '@ant-design/pro-layout'
import {Button, Card, Descriptions, Modal, Tooltip} from 'antd'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import AssignmentStatusTag from '../../components/AssignmentStatusTag'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import EntityLink from '../../components/EntityLink'
import {
  AssignmentStatus,
  GetAssignmentListQueryResult,
  useDeleteAssignmentMutation,
  useGetAssignmentListQuery
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import SortSelect from '../../components/SortSelect'
import {compare} from "../../services/util";

const { confirm } = Modal

const AssignmentListPage: React.FC = () => {
  const result = useGetAssignmentListQuery()
  const [deleteAssignment] = useDeleteAssignmentMutation()

  const sortVariants: Record<string, (a: Assignment, b: Assignment) => number> = {
    NEWEST: (a: Assignment, b: Assignment) => {
      const result = compare(a.createdAt, b.createdAt, value => Date.parse(value))

      // Reverse the order, if both exist
      // The list has to be reverse sorted, because newer timestamps are greater than older ones
      return (a.createdAt && b.createdAt) ? (-1 * result) : result
    },
    OLDEST: (a: Assignment, b: Assignment) => sortVariants['NEWEST'](b, a),
    TITLE: (a: Assignment, b: Assignment) => a.title.localeCompare(b.title),
    STATUS: (a: Assignment, b: Assignment) => {
      const statusOrder: Record<AssignmentStatus, number> = {
        INACTIVE: 0,
        ACTIVE: 1,
        OPEN: 2,
        CLOSED: 3
      }

      return compare(a.status, b.status, (value => statusOrder[value]))
    }
  }

  const sortValues: string[] = Object.keys(sortVariants)
  const [currentSortValue, setCurrentSortValue] = useState(sortValues[0])

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignments } = result.data

  const handleSortChange = ((value: string) => {
    setCurrentSortValue(value)
  })

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
          <>
            <SortSelect
              defaultValue={currentSortValue}
              values={sortValues}
              onSortChange={handleSortChange}
            />
            <Authorized authority="ROLE_TEACHER">
              <Link to="/assignments/create" key="1">
                <Button type="primary" icon="plus">
                  Create Assignment
                </Button>
              </Link>
            </Authorized>
          </>
        }
      />
      {
        assignments.slice()
          .sort(sortVariants[currentSortValue])
          .map(renderAssignment(renderProps))
      }
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
