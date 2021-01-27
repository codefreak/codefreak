import React, { useState } from 'react'
import {
  UploadTasksMutationResult,
  useImportTasksMutation,
  useUploadTasksMutation
} from '../services/codefreak-api'
import { UploadOutlined } from '@ant-design/icons'
import { Alert, Button, Modal } from 'antd'
import FileImport from './FileImport'
import { useInlineErrorMessage } from '../hooks/useInlineErrorMessage'

interface UploadTasksButtonProps {
  onUploadCompleted: (
    result: NonNullable<UploadTasksMutationResult['data']>['uploadTasks'] | null
  ) => void
}

const UploadTasksButton = (props: UploadTasksButtonProps) => {
  const [modalVisible, setModalVisible] = useState(false)
  const [inlineError, setErrorMessage] = useInlineErrorMessage(
    'Error while creating task(s)'
  )
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)

  const [uploadTasks, { loading: uploading }] = useUploadTasksMutation({
    context: { disableGlobalErrorHandling: true }
  })

  const [importTasks, { loading: importing }] = useImportTasksMutation({
    context: { disableGlobalErrorHandling: true }
  })

  const onUpload = (files: File[]) =>
    uploadTasks({ variables: { files } })
      .then(r => {
        const data = r.data ? r.data.uploadTasks : null
        if (data) {
          hideModal()
        }
        props.onUploadCompleted(data)
      })
      .catch(reason => setErrorMessage(reason.message))

  const onImport = (url: string) =>
    importTasks({ variables: { url } })
      .then(r => {
        const data = r.data ? r.data.importTasks : null
        if (data) {
          hideModal()
        }
        props.onUploadCompleted(data)
      })
      .catch(reason => setErrorMessage(reason.message))

  return (
    <>
      <Button icon={<UploadOutlined />} type="default" onClick={showModal}>
        Import Tasks
      </Button>
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Import tasks"
        footer={[
          <Button type="default" onClick={hideModal} key="cancel">
            Cancel
          </Button>
        ]}
        width="80%"
      >
        <Alert
          message="This action will create the imported tasks as new tasks and will not alter or delete existing tasks."
          style={{ marginBottom: 16, marginTop: 16 }}
        />
        {inlineError}
        <FileImport
          uploading={uploading}
          onUpload={onUpload}
          importing={importing}
          onImport={onImport}
        />
      </Modal>
    </>
  )
}

export default UploadTasksButton
