import { QueryResult } from '@apollo/react-common'
import { Button, Card, Modal } from 'antd'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import {
  TaskListItemFragment,
  useDeleteTaskMutation
} from '../services/codefreak-api'
import { messageService } from '../services/message'
import EntityLink from './EntityLink'
import EvaluationIndicator from './EvaluationIndicator'

const { confirm } = Modal

type Task = TaskListItemFragment & {
  answer?: any
}

interface RenderProps {
  delete: (taskId: string) => Promise<any>
}

const renderTask = (props: RenderProps) => (task: Task) => {
  const confirmDelete = () =>
    confirm({
      title: 'Do you want to delete this task from the task pool?',
      content: 'This cannot be undone! The task will be lost forever.',
      async onOk() {
        await props.delete(task.id)
      }
    })

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
      extra={
        <Button
          onClick={confirmDelete}
          type="dashed"
          shape="circle"
          icon="delete"
        />
      }
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

interface TaskListProps {
  tasks: Task[]
  update: () => void
}

const TaskList: React.FC<TaskListProps> = props => {
  const [deleteTask, deleteTaskResult] = useDeleteTaskMutation()
  const renderProps: RenderProps = {
    delete: async (id: string) => {
      const result = await deleteTask({ variables: { id } })
      if (result.data) {
        messageService.success('Task deleted')
        props.update()
      }
    }
  }
  return <>{props.tasks.map(renderTask(renderProps))}</>
}

export default TaskList
