import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Alert, Button, Card, Col, Row } from 'antd'
import React, { useState } from 'react'
import { useHistory } from 'react-router-dom'
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
import { getAllTemplates } from '../../services/templates'
import InlineError from '../../components/InlineError'

const CreateTaskPage: React.FC = () => {
  const taskTemplates = getAllTemplates()
  const [
    createTaskMutation,
    { loading: creatingTask }
  ] = useCreateTaskMutation()
  const history = useHistory()

  const [uploadTask, { loading: uploading }] = useUploadTaskMutation({
    context: { disableGlobalErrorHandling: true }
  })

  const [importTask, { loading: importing }] = useImportTaskMutation({
    context: { disableGlobalErrorHandling: true }
  })

  const [inlineErrorMessage, setInlineErrorMessage] = useState('')

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
    uploadTask({ variables: { files } })
      .then(r => uploadOrImportCompleted(r.data ? r.data.uploadTask : null))
      .catch(reason => setInlineErrorMessage(reason.message))

  const onImport = (url: string) =>
    importTask({ variables: { url } })
      .then(r => uploadOrImportCompleted(r.data ? r.data.importTask : null))
      .catch(reason => setInlineErrorMessage(reason.message))

  const createTask = (template?: TaskTemplate) => async () => {
    const result = await createTaskMutation({ variables: { template } })
    if (result.data) {
      onTaskCreated(result.data.createTask)
    }
  }

  const inlineError =
    inlineErrorMessage.length > 0 ? (
      <InlineError
        title="Error while importing task"
        message={inlineErrorMessage}
      />
    ) : null

  return (
    <>
      <PageHeaderWrapper />
      <Alert
        message="Tasks can only be created in the task pool. You can later add them to any assignment."
        style={{ marginBottom: 16 }}
      />
      <Row gutter={16}>
        {(Object.keys(taskTemplates) as TaskTemplate[]).map(templateKey => {
          const template = taskTemplates[templateKey]
          return (
            <Col
              xs={24}
              sm={12}
              md={6}
              xl={4}
              style={{ marginBottom: 16 }}
              key={templateKey}
            >
              <Card
                cover={
                  <div style={{ padding: '2em 2em 0' }}>
                    <template.logo className="language-logo" />
                  </div>
                }
                actions={[
                  <Button
                    key="1"
                    type="primary"
                    onClick={createTask(templateKey)}
                  >
                    Use this template
                  </Button>
                ]}
              >
                <Card.Meta
                  title={template.title}
                  description={template.description}
                />
              </Card>
            </Col>
          )
        })}
      </Row>
      <div style={{ marginBottom: 16 }}>
        <i>All trademarks are the property of their respective owners.</i>
      </div>
      <Card title="Import" style={{ marginBottom: 16 }}>
        {inlineError}
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
