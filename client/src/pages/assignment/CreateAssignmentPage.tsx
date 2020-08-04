import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Card, Modal } from 'antd'
import React from 'react'
import { useHistory } from 'react-router-dom'
import FileImport from '../../components/FileImport'
import HelpButton from '../../components/HelpButton'
import {
  UploadAssignmentMutationResult,
  useCreateAssignmentMutation,
  useImportAssignmentMutation,
  useUploadAssignmentMutation
} from '../../generated/graphql'
import { Entity, getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'

const CreateAssignmentPage: React.FC = () => {
  const [
    createAssignmentMutation,
    { loading: creatingAssignment }
  ] = useCreateAssignmentMutation()
  const history = useHistory()

  const [
    uploadAssignment,
    { loading: uploading }
  ] = useUploadAssignmentMutation()

  const [
    importAssignment,
    { loading: importing }
  ] = useImportAssignmentMutation()

  const renderTaskError = (error: string, index: number) => (
    <p key={index}>{error}</p>
  )

  const uploadOrImportCompleted = (
    result:
      | NonNullable<UploadAssignmentMutationResult['data']>['uploadAssignment']
      | null
  ) => {
    if (result) {
      history.push(getEntityPath(result.assignment))
      if (result.taskErrors.length > 0) {
        Modal.error({
          title:
            'The assigment has been created but not all tasks have been created successfully',
          content: <>{result.taskErrors.map(renderTaskError)}</>
        })
      } else {
        messageService.success('Assignment created')
      }
    }
  }

  const onUpload = (files: File[]) =>
    uploadAssignment({ variables: { files } }).then(r =>
      uploadOrImportCompleted(r.data ? r.data.uploadAssignment : null)
    )

  const onImport = (url: string) =>
    importAssignment({ variables: { url } }).then(r =>
      uploadOrImportCompleted(r.data ? r.data.importAssignment : null)
    )

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

  return (
    <>
      <PageHeaderWrapper />
      <Card title="From Scratch" style={{ marginBottom: 16 }}>
        <div style={{ textAlign: 'center' }}>
          <Button
            onClick={createAssignment}
            size="large"
            type="primary"
            loading={creatingAssignment}
            block
          >
            Create Empty Assignment
          </Button>
        </div>
      </Card>
      <Card
        title="Import"
        extra={
          <HelpButton category="definitions" section="assignment">
            File Format
          </HelpButton>
        }
      >
        <FileImport
          uploading={uploading}
          onUpload={onUpload}
          importing={importing}
          onImport={onImport}
        />
      </Card>
    </>
  )
}

export default CreateAssignmentPage
