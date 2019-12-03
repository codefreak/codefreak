import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import { createBreadcrumb } from '../../components/DefaultLayout'
import SetTitle from '../../components/SetTitle'
import { useGetAssignmentQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'
import { createRoutes } from '../../services/custom-breadcrump'

const AssignmentPage: React.FC = () => {
  const result = useGetAssignmentQuery({
    variables: { id: useIdParam() }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignment } = result.data

  return (
    <>
      <SetTitle>{assignment.title}</SetTitle>
      <PageHeaderWrapper
        title={assignment.title}
        tabList={[
          { key: 'tasks', tab: 'Tasks' },
          { key: 'submissions', tab: 'Submissions' }
        ]}
        tabActiveKey="tasks"
        breadcrumb={createBreadcrumb(createRoutes.forAssignment(assignment))}
      />
      <Link to={`/tasks/1337`}>Sample Task</Link>
    </>
  )
}

export default AssignmentPage
