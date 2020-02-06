import { Button, Card, Modal, Tooltip } from 'antd'
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
      title: 'Are you sure?',
      content: task.inPool
        ? 'You are deleting this task from the task pool. This cannot be undone!'
        : 'You are removing this task from the assignment. The original template will stay in the task pool. All other data (including student submissions) will be lost.',
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
        task.editable ? (
          <Tooltip
            title={task.inPool ? 'Delete from pool' : 'Remove from assignment'}
            placement="left"
          >
            <Button
              onClick={confirmDelete}
              type="dashed"
              shape="circle"
              icon="delete"
            />
          </Tooltip>
        ) : null
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
  const [deleteTask] = useDeleteTaskMutation()
  const renderProps: RenderProps = {
    delete: async (id: string) => {
      const result = await deleteTask({ variables: { id } })
      if (result.data) {
        messageService.success('Task removed')
        props.update()
      }
    }
  }
  return <>{props.tasks.map(renderTask(renderProps))}</>
}

export default TaskList
