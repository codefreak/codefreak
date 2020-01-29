import { Button, Card } from 'antd'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import { TaskListItemFragment } from '../services/codefreak-api'
import EntityLink from './EntityLink'
import EvaluationIndicator from './EvaluationIndicator'

type Task = TaskListItemFragment & {
  answer?: any
}

const renderTask = (task: Task) => {
  return (
    <Card
      title={
        <>
          {task.title}
          {task.answer ? (
            <EvaluationIndicator
              style={{ marginLeft: 8 }}
              answerId={task.answer.id}
            />
          ) : null}
        </>
      }
      key={task.id}
      style={{ marginBottom: 16 }}
    >
      {task.body ? <ReactMarkdown source={task.body} /> : null}
      <EntityLink to={task} sub={task.answer ? '/answer' : undefined}>
        <Button icon="folder-open" type="primary">
          Details
        </Button>
      </EntityLink>
    </Card>
  )
}

const TaskList: React.FC<{ tasks: Task[] }> = props => {
  return <>{props.tasks.map(renderTask)}</>
}

export default TaskList
