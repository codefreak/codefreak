import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Card } from 'antd'
import React from 'react'
import { useHistory } from 'react-router'
import FileImport from '../../components/FileImport'
import { useCreateAssignmentMutation } from '../../generated/graphql'
import { Entity, getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'

const CreateAssignmentPage: React.FC = () => {
  const [
    createAssignmentMutation,
    { loading: creatingAssignment }
  ] = useCreateAssignmentMutation()
  const history = useHistory()

  const onAssignmentCreated = (assignment: Entity) => {
    history.push(getEntityPath(assignment))
    messageService.success('Assignment created')
  }

  const createAssignment = async () => {
    const result = await createAssignmentMutation()
    if (result.data) {
      onAssignmentCreated(result.data.createAssignment)
    }
  }
  const noop = () => {
    // todo
  }
  return (
    <>
      <PageHeaderWrapper />
      <Card title="From Scratch">
        <div style={{ textAlign: 'center' }}>
          <Button
            onClick={createAssignment}
            size="large"
            loading={creatingAssignment}
            block
          >
            Create Empty Assignment
          </Button>
        </div>
      </Card>
      <Card title="Import" style={{ marginBottom: 16 }}>
        <FileImport
          uploading={false}
          onUpload={noop}
          importing={false}
          onImport={noop}
        />
      </Card>
    </>
  )
}

export default CreateAssignmentPage
