import { Alert, Col, Icon, Result, Row } from 'antd'
import React, { useState } from 'react'
import { FileType } from '../generated/graphql'
import {
  basename,
  FileSystemDirectoryNode,
  FileSystemNode
} from '../services/file'
import AnswerFileTree from './AnswerFileTree'
import Centered from './Centered'
import CodeViewer from './CodeViewer'

export interface AnswerFileBrowserProps {
  answerId: string
}

const renderTreeNodeContent = (
  answerId: string,
  file: FileSystemNode | FileSystemDirectoryNode
) => {
  if (file.type === FileType.File) {
    return <CodeViewer answerId={answerId} path={file.path} />
  }

  if (file.type === FileType.Directory) {
    return (
      <Centered>
        <Result title={file.path} icon={<Icon type="folder-open" />} />
      </Centered>
    )
  }
}

const AnswerFileBrowser: React.FC<AnswerFileBrowserProps> = ({ answerId }) => {
  const [selectedFile, setSelectedFile] = useState<FileSystemNode | undefined>()

  return (
    <Row gutter={16} type="flex">
      <Col span={6} style={{ backgroundColor: '#fafafa' }}>
        <AnswerFileTree answerId={answerId} onFileSelect={setSelectedFile} />
      </Col>
      <Col span={18}>
        {selectedFile ? (
          renderTreeNodeContent(answerId, selectedFile)
        ) : (
          <Centered>
            <Result
              title="Please select a file from the tree to view its content"
              icon={<Icon type="file" twoToneColor="red" />}
            />
          </Centered>
        )}
      </Col>
    </Row>
  )
}

export default AnswerFileBrowser
