import { Modal, Upload } from 'antd'
import { cloneFileWithRelativeName } from '../../services/uploads'
import { InboxOutlined, UploadOutlined } from '@ant-design/icons'
import React, { useCallback, useState } from 'react'
import { UploadFile } from 'antd/es/upload/interface'
import { DraggerProps, RcFile } from 'antd/es/upload'

interface UploadModalProps {
  title: React.ReactNode
  visible: boolean
  onCancel: () => void
  onUpload: (files: File[]) => void | Promise<unknown>
}

const UploadModal: React.FC<UploadModalProps> = props => {
  const { visible, onCancel, onUpload } = props
  const [fileUploadList, setFileUploadList] = useState<UploadFile[]>([])
  const beforeUpload: DraggerProps['beforeUpload'] = useCallback(
    (_, files) => {
      const filesToUpload = files.map((theFile: RcFile) =>
        cloneFileWithRelativeName(theFile)
      )
      setFileUploadList([...fileUploadList, ...filesToUpload])
      return false
    },
    [fileUploadList, setFileUploadList]
  )
  const onRemove: DraggerProps['onRemove'] = useCallback(
    fileToRemove => {
      setFileUploadList(
        fileUploadList.filter(file => file.uid !== fileToRemove.uid)
      )
    },
    [fileUploadList, setFileUploadList]
  )

  return (
    <Modal
      title={props.title}
      visible={visible}
      onCancel={onCancel}
      onOk={async () => {
        // ???
        await onUpload(fileUploadList as unknown as File[])
        setFileUploadList([])
      }}
      okText={
        fileUploadList.length === 0
          ? 'Upload files'
          : `Upload ${fileUploadList.length} files`
      }
      okButtonProps={{
        disabled: fileUploadList.length === 0,
        icon: <UploadOutlined />
      }}
    >
      <Upload.Dragger
        fileList={fileUploadList}
        multiple
        beforeUpload={beforeUpload}
        transformFile={cloneFileWithRelativeName}
        onRemove={onRemove}
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">
          Click or drag file to this area to upload
        </p>
      </Upload.Dragger>
    </Modal>
  )
}

export default UploadModal
