import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Alert, Button, Card } from 'antd'
import React from 'react'
import { useHistory } from 'react-router'
import FileImport from '../../components/FileImport'
import {
  TaskTemplate,
  UploadTaskMutationResult,
  useCreateTaskMutation,
  useImportTaskMutation,
  useUploadTaskMutation
} from '../../generated/graphql'
import { Entity, getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'

const TEMPLATES: { [key in TaskTemplate]: string } = {
  CSHARP: 'C#',
  JAVA: 'Java',
  JAVASCRIPT: 'JavaScript',
  PYTHON: 'Python'
}

const CreateTaskPage: React.FC = () => {
  const [
    createTaskMutation,
    { loading: creatingTask }
  ] = useCreateTaskMutation()
  const history = useHistory()

  const [uploadTask, { loading: uploading }] = useUploadTaskMutation()

  const [importTask, { loading: importing }] = useImportTaskMutation()

  const onTaskCreated = (task: Entity) => {
    history.push(getEntityPath(task))
    messageService.success('Task created')
  }

  const uploadOrImportCompleted = (
    result: NonNullable<UploadTaskMutationResult['data']>['uploadTask'] | null
  ) => {
    if (result) {
      history.push(getEntityPath(result))
      messageService.success('Task created')
    }
  }

  const onUpload = (files: File[]) =>
    uploadTask({ variables: { files } }).then(r =>
      uploadOrImportCompleted(r.data ? r.data.uploadTask : null)
    )

  const onImport = (url: string) =>
    importTask({ variables: { url } }).then(r =>
      uploadOrImportCompleted(r.data ? r.data.importTask : null)
    )

  const createTask = (template?: TaskTemplate) => async () => {
    const result = await createTaskMutation({ variables: { template } })
    if (result.data) {
      onTaskCreated(result.data.createTask)
    }
  }
  const gridStyle: React.CSSProperties = {
    width: '25%',
    textAlign: 'center'
  }
  return (
    <>
      <PageHeaderWrapper />
      <Alert
        message="Tasks can only be created in the task pool. You can later add them to any assignment."
        style={{ marginBottom: 16 }}
      />
      <Card title="From Template" style={{ marginBottom: 16 }}>
        {(Object.keys(TEMPLATES) as TaskTemplate[]).map(template => (
          <Card.Grid style={gridStyle} key={template}>
            <Button type="primary" onClick={createTask(template)}>
              {TEMPLATES[template]}
            </Button>
          </Card.Grid>
        ))}
      </Card>
      <Card title="Import" style={{ marginBottom: 16 }}>
        <FileImport
          uploading={uploading}
          onUpload={onUpload}
          importing={importing}
          onImport={onImport}
        />
      </Card>
      <Card title="From Scratch">
        <div style={{ textAlign: 'center' }}>
          <Button
            onClick={createTask()}
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

export default CreateTaskPage
