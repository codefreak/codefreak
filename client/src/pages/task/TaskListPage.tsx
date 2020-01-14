import { Button, Card } from 'antd'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EntityLink from '../../components/EntityLink'
import useIdParam from '../../hooks/useIdParam'
import {
  GetTaskListQueryResult,
  useGetTaskListQuery
} from '../../services/codefreak-api'

const TaskListPage: React.FC = () => {
  const assignmentId = useIdParam()
  const result = useGetTaskListQuery({ variables: { assignmentId } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { tasks } = result.data.assignment
  return <>{tasks.map(renderTask)}</>
}

const renderTask = (
  task: NonNullable<
    GetTaskListQueryResult['data']
  >['assignment']['tasks'][number]
) => {
  return (
    <Card title={task.title} key={task.id} style={{ marginBottom: 16 }}>
      {task.body ? <ReactMarkdown source={task.body} /> : null}
      <EntityLink to={task} sub={task.answer ? '/answer' : undefined}>
        <Button icon="folder-open" type="primary">
          Details
        </Button>
      </EntityLink>
    </Card>
  )
}

export default TaskListPage
