import React, { useState } from 'react'
import { useGetTaskPoolForAddingQuery } from '../services/codefreak-api'
import { Alert, Checkbox, Col, Empty, Row } from 'antd'
import AsyncPlaceholder from './AsyncContainer'
import {
  filterTasks,
  TaskSortMethodNames,
  TaskSortMethods
} from '../services/task'
import { Link } from 'react-router-dom'
import { CheckboxValueType } from 'antd/lib/checkbox/Group'
import SortSelect from './SortSelect'
import SearchBar from './SearchBar'

const TaskSelectionList: React.FC<{
  selectedTaskIds: string[]
  setSelectedTaskIds: (value: string[]) => void
  sortMethod: string
  filterCriteria: string
  maxHeight?: string | number | undefined
}> = props => {
  const result = useGetTaskPoolForAddingQuery()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  let taskPool = result.data.taskPool.slice()

  if (taskPool.length === 0) {
    return (
      <Empty description={<span>Your task pool is empty.</span>}>
        <Link to="/tasks/pool">Go to the task pool</Link> and create or import
        your first task!
      </Empty>
    )
  }

  if (props.filterCriteria) {
    taskPool = filterTasks(taskPool, props.filterCriteria)
  }

  taskPool = taskPool.sort(TaskSortMethods[props.sortMethod])

  const options = taskPool.map(task => ({
    label: task.title,
    value: task.id
  }))

  const onChange = (value: CheckboxValueType[]) =>
    props.setSelectedTaskIds(value as string[])

  return (
    <div style={{ maxHeight: props.maxHeight, overflowY: 'auto' }}>
      <Checkbox.Group
        className="vertical-checkbox-group"
        options={options}
        onChange={onChange}
        value={props.selectedTaskIds}
      />
    </div>
  )
}

const TaskSelection: React.FC<{
  selectedTaskIds: string[]
  setSelectedTaskIds: (value: string[]) => void
}> = props => {
  const [sortMethod, setSortMethod] = useState(TaskSortMethodNames[0])
  const [filterCriteria, setFilterCriteria] = useState('')

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

  return (
    <>
      <Row gutter={16}>
        <Col>{searchBar}</Col>
        <Col>{sorter}</Col>
      </Row>
      <Alert
        message={
          'When a task from the pool is added to an assignment, an independent copy is created. ' +
          'Editing the task in the pool will have no effect on the assignment and vice versa.'
        }
        style={{ marginBottom: 16, marginTop: 16 }}
      />
      <TaskSelectionList
        selectedTaskIds={props.selectedTaskIds}
        setSelectedTaskIds={props.setSelectedTaskIds}
        sortMethod={sortMethod}
        filterCriteria={filterCriteria}
        maxHeight="200px"
      />
    </>
  )
}

export default TaskSelection
