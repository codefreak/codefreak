import { Card } from 'antd'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import AsyncPlaceholder from '../../components/AsyncContainer'
import useIdParam from '../../hooks/useIdParam'
import { useGetTaskDetailsQuery } from '../../services/codefreak-api'

const TaskDetailsPage: React.FC = () => {
  const result = useGetTaskDetailsQuery({
    variables: { id: useIdParam() }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { task } = result.data

  return (
    <>
      {task.body ? (
        <Card title="Instructions">
          <ReactMarkdown source={task.body} />
        </Card>
      ) : null}
    </>
  )
}

export default TaskDetailsPage
