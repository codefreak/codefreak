import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Alert, Button, Card, Col, Row } from 'antd'
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

const TEMPLATES: {
  [key in TaskTemplate]: { title: string; description: string }
} = {
  CSHARP: { title: 'C#', description: '.NET, NUnit, Code Climate' },
  JAVA: { title: 'Java', description: 'JUnit, Code Climate' },
  JAVASCRIPT: { title: 'JavaScript', description: 'Jest, Code Climate' },
  PYTHON: { title: 'Python', description: 'pytest, Code Climate' }
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
  return (
    <>
      <PageHeaderWrapper />
      <Alert
        message="Tasks can only be created in the task pool. You can later add them to any assignment."
        style={{ marginBottom: 16 }}
      />
      <Row gutter={16} style={{ marginBottom: 8 }}>
        {(Object.keys(TEMPLATES) as TaskTemplate[]).map(template => (
          <Col span={4} key={template}>
            <Card
              cover={
                <img
                  alt="Logo"
                  src={`${
                    process.env.PUBLIC_URL
                  }/templates/${template.toLowerCase()}.png`}
                />
              }
              actions={[
                <Button key="1" type="primary" onClick={createTask(template)}>
                  Use this template
                </Button>
              ]}
            >
              <Card.Meta
                title={TEMPLATES[template].title}
                description={TEMPLATES[template].description}
              />
            </Card>
          </Col>
        ))}
      </Row>
      <div style={{ marginBottom: 16 }}>
        <i>All trademarks are the property of their respective owners.</i>
      </div>
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
