import { Button, Modal, Tooltip } from 'antd'
import { CardProps } from 'antd/lib/card'
import moment from 'moment'
import React from 'react'
import ReactMarkdown from 'react-markdown'
import {
  TaskListItemFragment,
  useDeleteTaskMutation,
  useSetTaskPositonMutation
} from '../services/codefreak-api'
import { messageService } from '../services/message'
import CardList from './CardList'
import CropContainer from './CropContainer'
import EntityLink from './EntityLink'
import EvaluationIndicator from './EvaluationIndicator'
import TimeLimitTag from './time-limit/TimeLimitTag'
import DateTag, { DateType } from './DateTag'
import Authorized from './Authorized';

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

  const createdAtTag = (
    <DateTag
      dateType={DateType.CREATED}
      contentType="TASK"
      date={new Date(task.createdAt)}
    />
  )

  const updatedAtTag = (
    <DateTag
      dateType={DateType.UPDATED}
      contentType="TASK"
      date={new Date(task.updatedAt)}
    />
  )

  const updateTimeDifference =
    Date.parse(task.updatedAt) - Date.parse(task.createdAt)
  const oneSecond = 1000

  const cardProps: CardProps = {
    title: (
      <>
        {task.title}
        {task.answer ? (
          <EvaluationIndicator
            style={{ marginLeft: 8 }}
            answerId={task.answer.id}
          />
        ) : null}{' '}
        {!!task.timeLimit ? (
          <TimeLimitTag
            timeLimit={task.timeLimit}
            deadline={
              task.answer && task.answer.deadline
                ? moment(task.answer.deadline)
                : undefined
            }
          />
        ) : null}
        <Authorized authority="ROLE_TEACHER">
          {createdAtTag}
          {updateTimeDifference >= oneSecond ? updatedAtTag : null}
        </Authorized>
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
        {task.body ? (
          <CropContainer maxHeight={100}>
            <ReactMarkdown source={task.body} />
          </CropContainer>
        ) : null}
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
    setTaskPosition({
      variables: { id: task.id, position: newPosition }
    }).then(() => messageService.success('Order updated'))

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
