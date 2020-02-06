import { PageHeaderWrapper } from '@ant-design/pro-layout'
import Emoji from 'a11y-react-emoji'
import { Button } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EmptyListCallToAction from '../../components/EmptyListCallToAction'
import TaskList from '../../components/TaskList'
import { useGetTaskPoolQuery } from '../../generated/graphql'

const TaskPoolPage: React.FC = () => {
  const result = useGetTaskPoolQuery({ fetchPolicy: 'cache-and-network' })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const tasks = result.data.taskPool

  const update = () => result.refetch()

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
      {tasks.length > 0 ? (
        <TaskList tasks={tasks} update={update} />
      ) : (
        <EmptyListCallToAction title="Your task pool is empty">
          Click here to create your first task! <Emoji symbol="✨" />
        </EmptyListCallToAction>
      )}
    </>
  )
}

export default TaskPoolPage
