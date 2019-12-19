import { Col, Icon, Row, Spin, Upload } from 'antd'
import { RcFile } from 'antd/lib/upload/interface'
import React from 'react'

const { Dragger } = Upload

interface FileImportProps {
  uploading: boolean
  onUpload: (files: File[]) => void
}

const FileImport: React.FC<FileImportProps> = props => {
  const { uploading, onUpload } = props

  const beforeUpload = (file: RcFile, fileList: RcFile[]) => {
    // this function is called for every file
    if (fileList.indexOf(file) === 0) {
      onUpload(fileList)
    }
    return false
  }

  return (
    <Row gutter={16}>
      <Col span={12}>
        <Dragger
          height={150}
          multiple
          showUploadList={false}
          beforeUpload={beforeUpload}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            {uploading ? <Spin size="large" /> : <Icon type="inbox" />}
          </p>
          <p className="ant-upload-text">
            Click or drag file to this area to upload
          </p>
          <p className="ant-upload-hint">
            Support for a single or bulk upload. To preserve the directory
            structure create a .zip, .tar or .tar.gz archive.
          </p>
        </Dragger>
      </Col>
      <Col span={6}>TODO: Git import</Col>
    </Row>
  )
}

export default FileImport
