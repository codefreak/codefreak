import { Col, Icon, Result, Row, Tabs } from 'antd'
import { basename } from 'path'
import React, { useState } from 'react'
import { FileType } from '../../services/codefreak-api'
import { FileTreeNode } from '../../services/file'
import Centered from '../Centered'
import CodeViewer from '../CodeViewer'
import AnswerFileTree from './AnswerFileTree'

import './index.less'

export interface AnswerFileBrowserProps {
  answerId: string
  review?: boolean
}

const AnswerFileBrowser: React.FC<AnswerFileBrowserProps> = ({
  answerId,
  review
}) => {
  const [openedFiles, setOpenedFiles] = useState<FileTreeNode[]>([])
  const [currentFile, setCurrentFile] = useState<FileTreeNode | undefined>()

  const onSelectFileInTree = (file: FileTreeNode | undefined) => {
    // only open files in editor
    if (!file || file.type !== FileType.File) {
      return
    }

    setCurrentFile(file)
    if (file && openedFiles.indexOf(file) === -1) {
      setOpenedFiles([...openedFiles, file])
    }
  }

  const onSelectTab = (path: string) => {
    setCurrentFile(openedFiles.find(file => file.path === path))
  }

  const onTabEdit = (
    path: string | React.MouseEvent<HTMLElement>,
    action: 'add' | 'remove'
  ) => {
    if (action === 'remove') {
      const newFiles = openedFiles.filter(file => file.path !== path)
      setOpenedFiles(newFiles)
      // switch to first tab by default after closing the current one
      setCurrentFile(newFiles.length ? newFiles[0] : undefined)
    }
  }

  return (
    <Row type="flex" className="answer-editor">
      <Col span={6} style={{ backgroundColor: '#fafafa' }}>
        <h4 className="answer-editor-file-title"> Files </h4>
        <AnswerFileTree answerId={answerId} onFileSelect={onSelectFileInTree} />
      </Col>
      <Col span={18} className="answer-editor-code-col">
        <Tabs
          animated={false}
          type="editable-card"
          hideAdd
          className="answer-editor-tabs"
          activeKey={currentFile ? currentFile.path : undefined}
          onTabClick={onSelectTab}
          onEdit={onTabEdit}
        >
          {openedFiles.map(file => (
            <Tabs.TabPane key={file.path} tab={basename(file.path)} />
          ))}
        </Tabs>
        <div className="answer-editor-content">
          {currentFile ? (
            <CodeViewer
              answerId={answerId}
              path={currentFile.path}
              review={review === true}
            />
          ) : (
            <Centered>
              <Result
                title="Please select a file from the tree to view its content"
                icon={<Icon type="file" twoToneColor="red" />}
              />
            </Centered>
          )}
        </div>
      </Col>
    </Row>
  )
}

export default AnswerFileBrowser
