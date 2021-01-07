import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Col, Row } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import {
  useDeleteAssignmentMutation,
  useGetAssignmentListQuery
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import SortSelect from '../../components/SortSelect'
import AssignmentList, {
  sortMethodNames
} from '../../components/AssignmentList'
import SearchBar from '../../components/SearchBar'

const AssignmentListPage: React.FC = () => {
  const result = useGetAssignmentListQuery()
  const [deleteAssignment] = useDeleteAssignmentMutation()

  const [sortMethod, setSortMethod] = useState(sortMethodNames[0])
  const [filterCriteria, setFilterCriteria] = useState('')

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { assignments } = result.data

  const handleSortChange = (value: string) => {
    setSortMethod(value)
  }

  const handleFilterChange = (value: string) => {
    setFilterCriteria(value)
  }

  const handleDelete = async (id: string) => {
    const deleteResult = await deleteAssignment({ variables: { id } })
    if (deleteResult.data) {
      messageService.success('Assignment removed')
      result.refetch()
    }
  }

  const sorter = (
    <SortSelect
      defaultValue={sortMethodNames[0]}
      values={sortMethodNames}
      onSortChange={handleSortChange}
    />
  )

  const searchBar = (
    <SearchBar
      searchType="Assignment"
      placeholder="for name..."
      onChange={handleFilterChange}
    />
  )

  const createButton = (
    <Authorized authority="ROLE_TEACHER">
      <Link to="/assignments/create" key="1">
        <Button type="primary" icon="plus">
          Create Assignment
        </Button>
      </Link>
    </Authorized>
  )

  return (
    <>
      <PageHeaderWrapper
        extra={
          <Row justify="end" gutter={16} type="flex">
            <Col>{searchBar}</Col>
            <Col>{sorter}</Col>
            <Col>{createButton}</Col>
          </Row>
        }
      />
      <AssignmentList
        list={assignments}
        sortMethod={sortMethod}
        filterCriteria={filterCriteria}
        onDelete={handleDelete}
      />
    </>
  )
}

export default AssignmentListPage
