import { Card } from 'antd'
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
      {task.body || editable ? (
        <Card title="Instructions">
          <EditableMarkdown
            content={task.body}
            editable={editable}
            onSave={updater('body')}
          />
        </Card>
      ) : null}
    </>
  )
}

export default TaskDetailsPage
