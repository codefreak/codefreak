import { Col, Icon, Result, Row, Tabs } from 'antd'
import React, { useState } from 'react'
import { FileType } from '../../services/codefreak-api'
import { basename, FileSystemDirectoryNode, FileSystemNode } from '../../services/file'
import AnswerFileTree from './AnswerFileTree'
import Centered from '../Centered'
import CodeViewer from '../CodeViewer'

import './index.less'

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
  const [openedFiles, setOpenedFiles] = useState<FileSystemNode[]>([])
  const [currentFile, setCurrentFile] = useState<FileSystemNode | undefined>()

  const onSelectFileInTree = (file: FileSystemNode | undefined) => {
    setCurrentFile(file)
    if (file && openedFiles.indexOf(file) === -1) {
      setOpenedFiles([...openedFiles, file])
    }
  }

  const onSelectTab = (path: string) => {
    setCurrentFile(openedFiles.find(file => file.path === path))
  }

  const onTabEdit = (path: string | React.MouseEvent<HTMLElement>, action: 'add' | 'remove') => {
    if (action === 'remove') {
      const newFiles = openedFiles.filter(file => file.path !== path)
      setOpenedFiles(newFiles)
      setCurrentFile(newFiles.length ? newFiles[0] : undefined)
    }
  }

  return (
    <Row type="flex" className="answer-editor">
      <Col span={6} style={{ backgroundColor: '#fafafa' }}>
        <AnswerFileTree answerId={answerId} onFileSelect={onSelectFileInTree} />
      </Col>
      <Col span={18} className="answer-editor-code-col">
        <Tabs
          animated={false}
          type="editable-card"
          hideAdd={true}
          className="answer-editor-tabs"
          activeKey={currentFile ? currentFile.path : undefined}
          onTabClick={onSelectTab}
          onEdit={onTabEdit}
        >
          {openedFiles.map(file => (
            <Tabs.TabPane key={file.path} tab={basename(file.path)} />
          ))}
        </Tabs>
        {currentFile ? (
          renderTreeNodeContent(answerId, currentFile)
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
