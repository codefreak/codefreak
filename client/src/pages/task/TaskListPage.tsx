import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import TaskList from '../../components/TaskList'
import useIdParam from '../../hooks/useIdParam'
import { useGetTaskListQuery } from '../../services/codefreak-api'

const TaskListPage: React.FC = () => {
  const assignmentId = useIdParam()
  const result = useGetTaskListQuery({ variables: { assignmentId } })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  return <TaskList tasks={result.data.assignment.tasks} />
}

export default TaskListPage
