import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import TaskList from '../../components/TaskList'
import { useGetTaskPoolQuery } from '../../generated/graphql'

const TaskPoolPage: React.FC = () => {
  const result = useGetTaskPoolQuery()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  return (
    <>
      <PageHeaderWrapper />
      <TaskList tasks={result.data.taskPool} />
    </>
  )
}

export default TaskPoolPage
