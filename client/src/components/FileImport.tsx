import { DownloadOutlined, InboxOutlined } from '@ant-design/icons'
import { Button, Col, Form, Input, Row, Spin, Upload } from 'antd'
import { RcFile } from 'antd/lib/upload/interface'
import React, { useCallback, useState } from 'react'
import useSystemConfig from '../hooks/useSystemConfig'
import { messageService } from '../services/message'
import Centered from './Centered'
import { findFilesWithInvalidExtension } from '../services/file'
import { useFormatter } from '../hooks/useFormatter'

const { Dragger } = Upload

interface FileImportProps {
  uploading: boolean
  onUpload: (files: File[]) => void
  importing: boolean
  onImport: (url: string) => void
  acceptedTypes?: string[]
}

const FileImport: React.FC<FileImportProps> = props => {
  const { uploading, onUpload, importing, onImport } = props
  const [url, setUrl] = useState('')
  const { bytes: formatBytes } = useFormatter()
  const { data: maxFileSize } = useSystemConfig('maxFileUploadSize')

  const beforeUpload = useCallback(
    (file: RcFile, fileList: RcFile[]) => {
      // this function is called for every file
      if (fileList.indexOf(file) === 0) {
        // The user might have switched to 'all files' instead of 'supported types' in the upload dialog
        if (props.acceptedTypes) {
          const invalidFiles = findFilesWithInvalidExtension(
            fileList.map(f => f.name),
            props.acceptedTypes
          )
          if (invalidFiles.length > 0) {
            messageService.error(
              `The following files have unsupported types: ${invalidFiles}`
            )
            return false
          }
        }

        if (maxFileSize && file.size > maxFileSize) {
          messageService.error(
            `Selected file is too large. Maximum allowed size is ${formatBytes(
              maxFileSize
            )}.`
          )
          return false
        }
        onUpload(fileList)
      }
      return false
    },
    [onUpload, maxFileSize, formatBytes, props.acceptedTypes]
  )

  const onUrlChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setUrl(e.target.value)
  const importSource = () => onImport(url) as void

  return (
    <Row gutter={16}>
      <Col span={12}>
        <Dragger
          multiple
          accept={props.acceptedTypes?.join(',')}
          showUploadList={false}
          beforeUpload={beforeUpload}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            {uploading ? <Spin size="large" /> : <InboxOutlined />}
          </p>
          <p className="ant-upload-text">
            Click or drag file to this area to upload
          </p>
          <p className="ant-upload-hint">
            Support for a single or bulk upload. To preserve the directory
            structure create a .zip, .tar or .tar.gz archive.
          </p>
          <p className="ant-upload-hint">
            Max. file size: {maxFileSize ? formatBytes(maxFileSize) : '…'}
          </p>
        </Dragger>
      </Col>
      <Col span={12}>
        <Centered style={{ height: 170 }}>
          <Form
            layout="horizontal"
            style={{ width: '100%', maxWidth: 700, marginBottom: -24 }}
          >
            <Form.Item
              label="Repository URL"
              labelCol={{ span: 6 }}
              wrapperCol={{ span: 12 }}
            >
              <Input
                onChange={onUrlChange}
                placeholder="ssh://git@example.org:1234/path/to/project.git"
              />
            </Form.Item>
            <Form.Item>
              <div style={{ textAlign: 'center' }}>
                <Button
                  icon={<DownloadOutlined />}
                  loading={importing}
                  onClick={importSource}
                >
                  Import source code from git
                </Button>
              </div>
            </Form.Item>
          </Form>
        </Centered>
      </Col>
    </Row>
  )
}

export default FileImport
