import {Button, Card, Descriptions, Modal, Tooltip} from 'antd'
import AssignmentStatusTag from './AssignmentStatusTag'
import EntityLink from './EntityLink'
import Authorized from './Authorized'
import React from 'react'
import {AssignmentStatus, GetAssignmentListQueryResult} from '../services/codefreak-api'
import {compare} from '../services/util'
import {matches} from '../services/strings'

const { confirm } = Modal

export type Assignment = NonNullable<
  GetAssignmentListQueryResult['data']
  >['assignments'][number]

const statusOrder: Record<AssignmentStatus, number> = {
  INACTIVE: 0,
  ACTIVE: 1,
  OPEN: 2,
  CLOSED: 3
}

const sortByNewest = (a: Assignment, b: Assignment) => {
  const result = compare(a.createdAt, b.createdAt, value => Date.parse(value))

  // Reverse the order, if both exist
  // The list has to be reverse sorted, because newer timestamps are greater than older ones
  return a.createdAt && b.createdAt ? -1 * result : result
}

export const sortMethods: Record<string, (a: Assignment, b: Assignment) => number> = {
  NEWEST: (a: Assignment, b: Assignment) => sortByNewest(a, b),
  OLDEST: (a: Assignment, b: Assignment) => sortByNewest(b, a),
  TITLE: (a: Assignment, b: Assignment) => a.title.localeCompare(b.title),
  STATUS: (a: Assignment, b: Assignment) =>
    compare(a.status, b.status, value => statusOrder[value])
}

export const sortMethodNames = Object.keys(sortMethods)

const filterAssignments = (list: Assignment[], criteria: string) => list.filter(assignment => matches(criteria, assignment.title))

interface AssignmentListProps {
  list: Assignment[]
  sortMethod?: string
  filterCriteria?: string
  onDelete: (assignmentId: string) => Promise<any>
}

const AssignmentList = (props: AssignmentListProps) => {
  let list = props.list

  if (props.filterCriteria) {
    list = filterAssignments(list, props.filterCriteria)
  }

  if (props.sortMethod) {
    list = list.slice().sort(sortMethods[props.sortMethod])
  }

  return (
    <>
      {list.map(renderAssignment({delete: props.onDelete}))}
    </>
  )
}

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

export default AssignmentList
