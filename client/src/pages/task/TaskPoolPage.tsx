import { PageHeaderWrapper } from '@ant-design/pro-layout'
import Emoji from 'a11y-react-emoji'
import { Button, Col, Row } from 'antd'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EmptyListCallToAction from '../../components/EmptyListCallToAction'
import TaskList from '../../components/TaskList'
import { useGetTaskPoolQuery } from '../../services/codefreak-api'
import ArchiveDownload from '../../components/ArchiveDownload'
import { messageService } from '../../services/message'
import UploadTasksButton from '../../components/UploadTasksButton'
import {
  filterTasks,
  TaskSortMethodNames,
  TaskSortMethods
} from '../../services/task'
import SortSelect from '../../components/SortSelect'
import SearchBar from '../../components/SearchBar'

const TaskPoolPage: React.FC = () => {
  const result = useGetTaskPoolQuery({ fetchPolicy: 'cache-and-network' })

  const [sortMethod, setSortMethod] = useState(TaskSortMethodNames[0])
  const [filterCriteria, setFilterCriteria] = useState('')

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  let tasks = result.data.taskPool.slice()

  if (filterCriteria) {
    tasks = filterTasks(tasks, filterCriteria)
  }

  tasks = tasks.sort(TaskSortMethods[sortMethod])

  const handleSortChange = (value: string) => setSortMethod(value)
  const handleFilterChange = (value: string) => setFilterCriteria(value)

  const sorter = (
    <SortSelect
      defaultValue={TaskSortMethodNames[0]}
      values={TaskSortMethodNames}
      onSortChange={handleSortChange}
    />
  )

  const searchBar = (
    <SearchBar
      searchType="Task"
      placeholder="by name..."
      onChange={handleFilterChange}
    />
  )

  const createButton = (
    <Link to="/tasks/pool/create">
      <Button type="primary" icon="plus">
        Create Task
      </Button>
    </Link>
  )

  const update = () => result.refetch()

  const handleUploadCompleted = (createdTasks: { id: string }[]) => {
    if (createdTasks.length === 0) {
      messageService.error('Tasks could not be created')
    } else {
      messageService.success(
        `${createdTasks.length} task(s) successfully created`
      )
    }
    result.refetch()
  }

  const exportButton = (
    <ArchiveDownload url="/api/tasks/export">Export Tasks</ArchiveDownload>
  )

  const importButton = (
    <UploadTasksButton onUploadCompleted={handleUploadCompleted} />
  )

  return (
    <>
      <PageHeaderWrapper
        extra={
          <Row justify="end" gutter={16} type="flex">
            <Col>{searchBar}</Col>
            <Col>{sorter}</Col>
            <Col>{exportButton}</Col>
            <Col>{importButton}</Col>
            <Col>{createButton}</Col>
          </Row>
        }
      />
      {tasks.length > 0 ? (
        <TaskList tasks={tasks} update={update} />
      ) : (
        <EmptyListCallToAction title="Your task pool is empty">
          Click here to create your first task! <Emoji symbol="✨" />
        </EmptyListCallToAction>
      )}
    </>
  )
}

export default TaskPoolPage
