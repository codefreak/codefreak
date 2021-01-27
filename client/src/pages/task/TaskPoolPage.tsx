import { PageHeaderWrapper } from '@ant-design/pro-layout'
import Emoji from 'a11y-react-emoji'
import { PlusOutlined } from '@ant-design/icons'
import { Button, Col, Row } from 'antd'
import { useState } from 'react'
import { Link, useHistory } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import EmptyListCallToAction from '../../components/EmptyListCallToAction'
import TaskList from '../../components/TaskList'
import {
  UploadTasksMutationResult,
  useGetTaskPoolQuery
} from '../../services/codefreak-api'
import ArchiveDownload from '../../components/ArchiveDownload'
import { messageService } from '../../services/message'
import ImportTasksButton from '../../components/ImportTasksButton'
import {
  filterTasks,
  TaskSortMethodNames,
  TaskSortMethods
} from '../../services/task'
import SortSelect from '../../components/SortSelect'
import SearchBar from '../../components/SearchBar'
import { getEntityPath } from '../../services/entity-path'

const TaskPoolPage: React.FC = () => {
  const result = useGetTaskPoolQuery({ fetchPolicy: 'cache-and-network' })

  const [sortMethod, setSortMethod] = useState(TaskSortMethodNames[0])
  const [filterCriteria, setFilterCriteria] = useState('')

  const history = useHistory()

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
      <Button type="primary" icon={<PlusOutlined />}>
        Create Task
      </Button>
    </Link>
  )

  const update = () => result.refetch()

  const handleImportCompleted = (
    createdTasks:
      | NonNullable<UploadTasksMutationResult['data']>['uploadTasks']
      | null
  ) => {
    if (createdTasks) {
      switch (createdTasks.length) {
        case 0:
          messageService.error('Tasks could not be created')
          break
        case 1:
          history.push(getEntityPath(createdTasks[0]))
          messageService.success('Task created')
          break
        default:
          messageService.success(
            `${createdTasks.length} task(s) successfully created`
          )
          result.refetch()
          break
      }
    }
  }

  const exportButton = (
    <ArchiveDownload url="/api/tasks/export">Export Tasks</ArchiveDownload>
  )

  const importButton = (
    <ImportTasksButton onImportCompleted={handleImportCompleted} />
  )

  return (
    <>
      <PageHeaderWrapper
        extra={
          <Row justify="end" gutter={16}>
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
