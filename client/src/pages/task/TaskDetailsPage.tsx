import { Card, Empty } from 'antd'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EditableMarkdown from '../../components/EditableMarkdown'
import useIdParam from '../../hooks/useIdParam'
import { TaskInput, useGetTaskDetailsQuery } from '../../services/codefreak-api'
import { Updater } from '../../services/util'

const TaskDetailsPage: React.FC<{
  updater: Updater<TaskInput, any>
  editable: boolean
}> = ({ updater, editable }) => {
  const result = useGetTaskDetailsQuery({
    variables: { id: useIdParam() }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  return (
    <>
      <Card title="Instructions">
        {task.body || editable ? (
          <EditableMarkdown
            content={task.body}
            editable={editable}
            onSave={updater('body')}
          />
        ) : (
          <Empty description="This task has no extra instructions. Take a look at the provided files." />
        )}
      </Card>
    </>
  )
}

export default TaskDetailsPage
