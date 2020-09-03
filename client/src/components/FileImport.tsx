import { Button, Col, Form, Icon, Input, Row, Spin, Upload } from 'antd'
import { RcFile } from 'antd/lib/upload/interface'
import filesize from 'filesize'
import React, { useCallback, useState } from 'react'
import useSystemConfig from '../hooks/useSystemConfig'
import { messageService } from '../services/message'
import Centered from './Centered'

const { Dragger } = Upload

interface FileImportProps {
  uploading: boolean
  onUpload: (files: File[]) => void
  importing: boolean
  onImport: (url: string) => void
}

const FileImport: React.FC<FileImportProps> = props => {
  const { uploading, onUpload, importing, onImport } = props
  const [url, setUrl] = useState('')
  const { data: maxFileSize } = useSystemConfig('maxFileUploadSize')

  const beforeUpload = useCallback(
    (file: RcFile, fileList: RcFile[]) => {
      // this function is called for every file
      if (fileList.indexOf(file) === 0) {
        if (maxFileSize && file.size > maxFileSize) {
          messageService.error(
            `Selected file is too large. Maximum allowed size is ${filesize(
              maxFileSize
            )}.`
          )
          return false
        }
        onUpload(fileList)
      }
      return false
    },
    [onUpload, maxFileSize]
  )

  const onUrlChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setUrl(e.target.value)
  const importSource = () => onImport(url) as void

  return (
    <Row gutter={16}>
      <Col span={12}>
        <Dragger
          height={170}
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
          <p className="ant-upload-hint">
            Max. file size: {maxFileSize ? filesize(maxFileSize) : '…'}
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
                  icon="download"
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
