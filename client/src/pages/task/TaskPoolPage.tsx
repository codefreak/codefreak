import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
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
      <PageHeaderWrapper
        extra={
          <Link to="/tasks/pool/create">
            <Button type="primary" icon="plus">
              Create Task
            </Button>
          </Link>
        }
      />
      <TaskList tasks={result.data.taskPool} />
    </>
  )
}

export default TaskPoolPage
