import { Col, Icon, Row, Tabs, Upload } from 'antd'
import React from 'react'

const { TabPane } = Tabs
const { Dragger } = Upload

interface FileImportProps {
  //
}

const FileImport: React.FC<FileImportProps> = props => {
  return (
    <Row gutter={16}>
      <Col span={12}>
        <div style={{ height: 150 }}>
          <Dragger>
            <p className="ant-upload-drag-icon">
              <Icon type="inbox" />
            </p>
            <p className="ant-upload-text">
              Click or drag file to this area to upload
            </p>
            <p className="ant-upload-hint">
              Support for a single or bulk upload. To preserve the directory
              structure create a .zip, .tar or .tar.gz archive.
            </p>
          </Dragger>
        </div>
      </Col>
      <Col span={6}>TODO: Git import</Col>
    </Row>
  )
}

export default FileImport
