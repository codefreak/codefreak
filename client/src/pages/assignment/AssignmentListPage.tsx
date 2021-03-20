import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Col, Modal, Row } from 'antd'
import { useState } from 'react'
import { useHistory } from 'react-router-dom'
import AsyncPlaceholder from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import {
  UploadAssignmentMutationResult,
  useDeleteAssignmentMutation,
  useGetAssignmentListQuery
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import SortSelect from '../../components/SortSelect'
import AssignmentList, {
  sortMethodNames
} from '../../components/AssignmentList'
import SearchBar from '../../components/SearchBar'
import ImportAssignmentButton from '../../components/ImportAssignmentButton'
import { getEntityPath } from '../../services/entity-path'
import CreateAssignmentButton from '../../components/CreateAssignmentButton'

const AssignmentListPage: React.FC = () => {
  const result = useGetAssignmentListQuery()
  const [deleteAssignment] = useDeleteAssignmentMutation()

  const [sortMethod, setSortMethod] = useState(sortMethodNames[0])
  const [filterCriteria, setFilterCriteria] = useState('')

  const history = useHistory()

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

  const renderTaskError = (error: string, index: number) => (
    <p key={index}>{error}</p>
  )

  const handleImportCompleted = (
    uploadResult:
      | NonNullable<UploadAssignmentMutationResult['data']>['uploadAssignment']
      | null
  ) => {
    if (uploadResult) {
      const assignment = uploadResult.assignment
      const taskCreationErrors = uploadResult.taskErrors

      history.push(getEntityPath(assignment))

      if (taskCreationErrors.length > 0) {
        Modal.error({
          title:
            'The assigment has been created but not all tasks have been created successfully',
          content: <>{taskCreationErrors.map(renderTaskError)}</>
        })
      } else {
        messageService.success('Assignment created')
      }
    }
  }

  const importButton = (
    <Authorized authority="ROLE_TEACHER">
      <ImportAssignmentButton onImportCompleted={handleImportCompleted} />
    </Authorized>
  )

  const createButton = (
    <Authorized authority="ROLE_TEACHER">
      <CreateAssignmentButton />
    </Authorized>
  )

  return (
    <>
      <PageHeaderWrapper
        extra={
          <Row justify="end" gutter={16}>
            <Col>{searchBar}</Col>
            <Col>{sorter}</Col>
            <Col>{importButton}</Col>
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
