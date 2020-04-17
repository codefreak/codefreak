import Emoji from 'a11y-react-emoji'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EmptyListCallToAction from '../../components/EmptyListCallToAction'
import TaskList from '../../components/TaskList'
import useIdParam from '../../hooks/useIdParam'
import { useGetTaskListQuery } from '../../services/codefreak-api'

const TaskListPage: React.FC = () => {
  const assignmentId = useIdParam()
  const result = useGetTaskListQuery({ variables: { assignmentId } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { tasks, editable } = result.data.assignment

  const update = () => result.refetch()

  return tasks.length === 0 && editable ? (
    <EmptyListCallToAction title="This assignment does not have any tasks yet">
      Click here to add the first task! <Emoji symbol="✨" />
    </EmptyListCallToAction>
  ) : (
    <TaskList tasks={tasks} update={update} sortable={editable} />
  )
}

export default TaskListPage
