import { Button, Card, Icon, Modal, Tooltip } from 'antd'
import React, { useEffect, useState } from 'react'
import {
  DragDropContext,
  Draggable,
  DraggableProvided,
  DraggableProvidedDragHandleProps,
  Droppable,
  DroppableProvided,
  DroppableStateSnapshot,
  DropResult
} from 'react-beautiful-dnd'
import ReactMarkdown from 'react-markdown'
import {
  TaskListItemFragment,
  useDeleteTaskMutation,
  useSetTaskPositonMutation
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

const renderTask = (
  props: RenderProps,
  dragHandleProps?: DraggableProvidedDragHandleProps
) => (task: Task) => {
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
          {dragHandleProps ? (
            <>
              <Icon
                type="drag"
                style={{ cursor: 'grab' }}
                {...dragHandleProps}
              />{' '}
              {task.title}
            </>
          ) : (
            task.title
          )}
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
  sortable?: boolean
}

const TaskList: React.FC<TaskListProps> = props => {
  const [tasks, setTasks] = useState<Task[]>([])
  const [deleteTask] = useDeleteTaskMutation()
  const [setTaskPosition] = useSetTaskPositonMutation()
  useEffect(() => setTasks(props.tasks), [props.tasks, setTasks])
  const renderProps: RenderProps = {
    delete: async (id: string) => {
      const result = await deleteTask({ variables: { id } })
      if (result.data) {
        messageService.success('Task removed')
        props.update()
      }
    }
  }
  const onDragEnd = (result: DropResult) => {
    if (
      !result.destination ||
      result.destination.index === result.source.index
    ) {
      return
    }

    // preemptively update the order to prevent flickering
    const newTasks = [...tasks]
    const [removed] = newTasks.splice(result.source.index, 1)
    newTasks.splice(result.destination.index, 0, removed)
    // setTasks(newTasks)

    setTaskPosition({
      variables: { id: result.draggableId, position: result.destination.index }
    })
      .then(() => messageService.success('Order updated'))
      .finally(props.update)
  }
  if (props.sortable && tasks.length > 1) {
    return (
      <DragDropContext onDragEnd={onDragEnd}>
        <Droppable droppableId="droppable">
          {(provided: DroppableProvided, snapshot: DroppableStateSnapshot) => (
            <div {...provided.droppableProps} ref={provided.innerRef}>
              {tasks.map(task => (
                <Draggable
                  key={task.id}
                  draggableId={task.id}
                  index={task.position}
                >
                  {(draggableProvided: DraggableProvided) => (
                    <div
                      ref={draggableProvided.innerRef}
                      {...draggableProvided.draggableProps}
                      style={draggableProvided.draggableProps.style}
                    >
                      {renderTask(
                        renderProps,
                        draggableProvided.dragHandleProps
                      )(task)}
                    </div>
                  )}
                </Draggable>
              ))}
              {provided.placeholder}
            </div>
          )}
        </Droppable>
      </DragDropContext>
    )
  }
  return <>{tasks.map(renderTask(renderProps))}</>
}

export default TaskList
