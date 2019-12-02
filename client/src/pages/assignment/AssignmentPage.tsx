import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import SetTitle from '../../components/SetTitle'
import { useGetAssignmentQuery } from '../../generated/graphql'
import useIdParam from '../../hooks/useIdParam'

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
      />
    </>
  )
}

export default AssignmentPage
