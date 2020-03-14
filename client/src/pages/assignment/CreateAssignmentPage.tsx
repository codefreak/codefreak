import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Alert, Button, Card } from 'antd'
import React from 'react'
import { useHistory } from 'react-router'
import FileImport from '../../components/FileImport'
import { useCreateTaskMutation } from '../../generated/graphql'
import { Entity, getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'

const CreateAssignmentPage: React.FC = () => {
  const [
    createTaskMutation,
    { loading: creatingTask }
  ] = useCreateTaskMutation()
  const history = useHistory()

  const onTaskCreated = (task: Entity) => {
    history.push(getEntityPath(task))
    messageService.success('Task created')
  }

  const createTask = async () => {
    const result = await createTaskMutation()
    if (result.data) {
      onTaskCreated(result.data.createTask)
    }
  }
  const noop = () => {
    // todo
  }
  return (
    <>
      <PageHeaderWrapper />
      <Alert
        message="Tasks can only be created in the task pool. You can later add them to any assignment."
        style={{ marginBottom: 16 }}
      />
      <Card title="From Template" style={{ marginBottom: 16 }}>
        TODO
      </Card>
      <Card title="Import" style={{ marginBottom: 16 }}>
        <FileImport
          uploading={false}
          onUpload={noop}
          importing={false}
          onImport={noop}
        />
      </Card>
      <Card title="From Scratch">
        <div style={{ textAlign: 'center' }}>
          <Button
            onClick={createTask}
            size="large"
            loading={creatingTask}
            block
          >
            Create Empty Task
          </Button>
        </div>
      </Card>
    </>
  )
}

export default CreateAssignmentPage
