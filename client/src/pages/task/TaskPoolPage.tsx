import { PageHeaderWrapper } from '@ant-design/pro-layout'
import Emoji from 'a11y-react-emoji'
import { Button } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EmptyListCallToAction from '../../components/EmptyListCallToAction'
import TaskList from '../../components/TaskList'
import { useGetTaskPoolQuery } from '../../services/codefreak-api'
import ArchiveDownload from '../../components/ArchiveDownload'
import { messageService } from '../../services/message'
import UploadTasksButton from '../../components/UploadTasksButton'

const TaskPoolPage: React.FC = () => {
  const result = useGetTaskPoolQuery({ fetchPolicy: 'cache-and-network' })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const tasks = result.data.taskPool

  const update = () => result.refetch()

  const handleUploadCompleted = (success: boolean) => {
    success
      ? messageService.success('Tasks created')
      : messageService.error('Tasks could not be created')
    result.refetch()
  }

  return (
    <>
      <PageHeaderWrapper
        extra={[
          <ArchiveDownload url="/api/tasks/export">
            Export Tasks
          </ArchiveDownload>,
          <UploadTasksButton onUploadCompleted={handleUploadCompleted} />,
          <Link to="/tasks/pool/create">
            <Button type="primary" icon="plus">
              Create Task
            </Button>
          </Link>
        ]}
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
