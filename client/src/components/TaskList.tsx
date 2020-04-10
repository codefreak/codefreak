import { Button, Modal, Tooltip } from 'antd'
import { CardProps } from 'antd/lib/card'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import {
  TaskListItemFragment,
  useDeleteTaskMutation,
  useSetTaskPositonMutation
} from '../services/codefreak-api'
import { messageService } from '../services/message'
import CardList from './CardList'
import EntityLink from './EntityLink'
import EvaluationIndicator from './EvaluationIndicator'

const { confirm } = Modal

type Task = TaskListItemFragment & {
  answer?: any
}

interface RenderProps {
  delete: (taskId: string) => Promise<any>
}

const renderTask = (props: RenderProps, dragHandleProps?: any) => (
  task: Task
) => {
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

  const cardProps: CardProps = {
    title: (
      <>
        {task.title}
        {task.answer ? (
          <EvaluationIndicator
            style={{ marginLeft: 8 }}
            answerId={task.answer.id}
          />
        ) : null}
      </>
    ),
    extra: task.editable ? (
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
    ) : null,
    children: (
      <>
        {task.body ? <ReactMarkdown source={task.body} /> : null}
        <EntityLink to={task} sub={task.answer ? '/answer' : undefined}>
          <Button icon="folder-open" type="primary">
            Details
          </Button>
        </EntityLink>
      </>
    )
  }

  return cardProps
}

interface TaskListProps {
  tasks: Task[]
  update: () => void
  sortable?: boolean
}

const TaskList: React.FC<TaskListProps> = props => {
  const [deleteTask] = useDeleteTaskMutation()
  const [setTaskPosition] = useSetTaskPositonMutation()
  const renderProps: RenderProps = {
    delete: async (id: string) => {
      const result = await deleteTask({ variables: { id } })
      if (result.data) {
        messageService.success('Task removed')
        props.update()
      }
    }
  }

  const handlePositionChange = (task: Task, newPosition: number) =>
    setTaskPosition({ variables: { id: task.id, position: newPosition } }).then(
      () => messageService.success('Order updated')
    )

  return (
    <CardList
      sortable={props.sortable}
      items={props.tasks}
      renderItem={renderTask(renderProps)}
      handlePositionChange={handlePositionChange}
    />
  )
}

export default TaskList
