import React, { useState } from 'react'
import {
  UploadAssignmentMutationResult,
  useImportAssignmentMutation,
  useUploadAssignmentMutation
} from '../services/codefreak-api'
import { UploadOutlined } from '@ant-design/icons'
import { Button, Modal } from 'antd'
import FileImport from './FileImport'
import { useInlineErrorMessage } from '../hooks/useInlineErrorMessage'

interface UploadAssignmentButtonProps {
  onUploadCompleted: (
    result:
      | NonNullable<UploadAssignmentMutationResult['data']>['uploadAssignment']
      | null
  ) => void
}

const UploadAssignmentButton = (props: UploadAssignmentButtonProps) => {
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)
  const [inlineError, setErrorMessage] = useInlineErrorMessage(
    'Error while importing assignment'
  )

  const [uploadTasks, { loading: uploading }] = useUploadAssignmentMutation({
    context: { disableGlobalErrorHandling: true }
  })

  const [importTasks, { loading: importing }] = useImportAssignmentMutation({
    context: { disableGlobalErrorHandling: true }
  })

  const onUpload = (files: File[]) =>
    uploadTasks({ variables: { files } })
      .then(r => {
        const data = r.data ? r.data.uploadAssignment : null
        if (data) {
          hideModal()
        }
        props.onUploadCompleted(data)
      })
      .catch(reason => setErrorMessage(reason.message))

  const onImport = (url: string) =>
    importTasks({ variables: { url } })
      .then(r => {
        const data = r.data ? r.data.importAssignment : null
        if (data) {
          hideModal()
        }
        props.onUploadCompleted(data)
      })
      .catch(reason => setErrorMessage(reason.message))

  return (
    <>
      <Button icon={<UploadOutlined />} type="default" onClick={showModal}>
        Import Assignment
      </Button>
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Import assignment"
        footer={[
          <Button type="default" onClick={hideModal} key="cancel">
            Cancel
          </Button>
        ]}
        width="80%"
      >
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

export default UploadAssignmentButton
